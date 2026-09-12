package com.idleflowgames.playgames

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.result.ActivityResult
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.games.AnnotatedData
import com.google.android.gms.games.GamesClientStatusCodes
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.games.snapshot.SnapshotMetadataBuffer
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.android.gms.tasks.Tasks
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * PGS Saved Games (Snapshots). A conflict is resolved by the policy the call names,
 * defaulting to most-recently-modified. The SDK's manual-resolution path is not bound,
 * so one side always wins outright and nothing is merged.
 */
internal class SavedGamesModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getSnapshotsClient(activity)

    fun load(call: PluginCall) {
        val name = call.requireString("name") ?: return
        val policy = call.conflictPolicyOption() ?: return
        // Blocking snapshot I/O off the main thread (GMS Task listeners default to UI).
        // Reading must not create: a caller asking for a save that is not there wants
        // null back, not an empty snapshot left behind in the player's list.
        client.open(name, /* createIfNotFound = */ false, policy)
            .addOnSuccessListener(IO_EXECUTOR) { result ->
                val snap = result.data
                if (snap == null) {
                    call.resolve(jsObject { put("snapshot", JSObject.NULL) })
                    return@addOnSuccessListener
                }
                val bytes = try {
                    snap.snapshotContents.readFully()
                } catch (e: IOException) {
                    client.discardAndClose(snap)
                    call.rejectFromException(e, "snapshot read failed")
                    return@addOnSuccessListener
                }
                val payload = snap.metadata.toJsObject().apply {
                    put("data", String(bytes, StandardCharsets.UTF_8))
                }
                client.discardAndClose(snap)
                call.resolve(jsObject { put("snapshot", payload) })
            }
            .addOnFailureListener(IO_EXECUTOR) { e ->
                if ((e as? ApiException)?.statusCode == GamesClientStatusCodes.SNAPSHOT_NOT_FOUND) {
                    call.resolve(jsObject { put("snapshot", JSObject.NULL) })
                } else {
                    call.rejectFromException(e, "snapshot load failed")
                }
            }
    }

    fun save(call: PluginCall) {
        val name = call.requireString("name") ?: return
        val data = call.requireString("data") ?: return
        val policy = call.conflictPolicyOption() ?: return
        val change = SnapshotMetadataChange.Builder()
            .setDescription(call.getString("description") ?: "")
        if (!call.readLong("playedTimeMillis") { change.setPlayedTimeMillis(it) }) return
        if (!call.readLong("progressValue") { change.setProgressValue(it) }) return
        val coverImage = call.getString("coverImage")
        if (coverImage != null) {
            val bitmap = decodeCoverImage(coverImage)
                ?: return call.reject("coverImage is not base64 that decodes to an image")
            change.setCoverImage(bitmap)
        }
        val metadataChange = change.build()

        // Blocking snapshot I/O off the main thread.
        client.open(name, /* createIfNotFound = */ true, policy)
            .continueWithTask(IO_EXECUTOR) { task ->
                val snap = task.result?.data
                    ?: throw IllegalStateException("snapshot unavailable")
                if (!snap.snapshotContents.writeBytes(data.toByteArray(StandardCharsets.UTF_8))) {
                    // writeBytes reports failure by return value; committing anyway would
                    // resolve the call on contents that were never written.
                    client.discardAndClose(snap)
                    throw IOException("snapshot write failed")
                }
                client.commitAndClose(snap, metadataChange)
            }
            .bind(call, "snapshot save failed")
    }

    fun list(call: PluginCall) {
        val forceReload = call.forceReload()
        client.load(forceReload).bind(call, "snapshot list failed") { result ->
            jsObject {
                put(
                    "snapshots",
                    result.get()?.use { it.toJsArray(SnapshotMetadata::toJsObject) } ?: JSArray(),
                )
            }
        }
    }

    fun delete(call: PluginCall) {
        val name = call.requireString("name") ?: return
        // The cached list can predate a snapshot written this session, so a miss is
        // re-checked against a forced reload before it is reported as absent.
        client.load(false)
            .continueWithTask { cached ->
                val meta = cached.result?.findSnapshot(name)
                if (meta != null) {
                    client.delete(meta)
                } else {
                    client.load(true).continueWithTask { reloaded ->
                        val fresh = reloaded.result?.findSnapshot(name)
                            ?: throw NoSuchElementException("snapshot '$name' not found")
                        client.delete(fresh)
                    }
                }
            }
            .bind(call, "snapshot delete failed")
    }

    fun show(call: PluginCall) {
        val title = call.getString("title") ?: DEFAULT_PICKER_TITLE
        val allowAdd = call.boolOption("allowAdd", true)
        val allowDelete = call.boolOption("allowDelete", true)
        val maxSnapshots = call.intOption("maxSnapshots", SnapshotsClient.DISPLAY_LIMIT_NONE) ?: return
        plugin.launchUiIntent(
            client.getSelectSnapshotIntent(title, allowAdd, allowDelete, maxSnapshots),
            call,
            "onSnapshotUiResult",
        )
    }

    fun limits(call: PluginCall) {
        val maxDataSize = client.maxDataSize
        val maxCoverImageSize = client.maxCoverImageSize
        Tasks.whenAll(maxDataSize, maxCoverImageSize).bind(call, "snapshot limits failed") { _ ->
            jsObject {
                put("maxDataSize", maxDataSize.result)
                put("maxCoverImageSize", maxCoverImageSize.result)
            }
        }
    }

    /** Resolve a `showSnapshots` call from the picker result; nothing picked yields null. */
    fun resolveSelection(call: PluginCall, result: ActivityResult) {
        val extras = result.data?.extras
        val isNew = extras?.getBoolean(SnapshotsClient.EXTRA_SNAPSHOT_NEW, false) ?: false
        val meta = if (isNew || extras == null) null else SnapshotsClient.getSnapshotFromBundle(extras)
        call.resolve(
            jsObject {
                put("snapshot", meta?.toJsObject() ?: JSObject.NULL)
                put("isNew", isNew)
            },
        )
    }

    private companion object {
        const val DEFAULT_PICKER_TITLE = "Saved games"

        // Serial daemon thread for snapshot file I/O.
        val IO_EXECUTOR: Executor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "pgs-saved-games").apply { isDaemon = true }
        }
    }
}

private fun PluginCall.conflictPolicyOption(): Int? =
    enumOption(
        "conflictPolicy",
        SNAPSHOT_CONFLICT_POLICIES,
        SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED,
    )

private fun AnnotatedData<SnapshotMetadataBuffer>.findSnapshot(name: String): SnapshotMetadata? =
    get()?.use { buffer -> buffer.firstOrNull { it.uniqueName == name }?.freeze() }

private fun decodeCoverImage(base64: String): Bitmap? {
    val bytes = try {
        Base64.decode(base64, Base64.DEFAULT)
    } catch (e: IllegalArgumentException) {
        return null
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

internal fun SnapshotMetadata.toJsObject(): JSObject = jsObject {
    put("name", uniqueName)
    put("snapshotId", snapshotId)
    put("description", description.orEmpty())
    put("modifiedAt", lastModifiedTimestamp)
    putUnlessSentinel("playedTimeMillis", playedTime, SnapshotMetadata.PLAYED_TIME_UNKNOWN)
    putUnlessSentinel("progressValue", progressValue, SnapshotMetadata.PROGRESS_VALUE_UNKNOWN)
    putIfPresent("deviceName", deviceName)
    coverImageUri?.let {
        put("coverImageUrl", it.toString())
        put("coverImageAspectRatio", coverImageAspectRatio.toDouble())
    }
    put("hasChangePending", hasChangePending())
}
