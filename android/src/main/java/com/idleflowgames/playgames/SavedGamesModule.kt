package com.idleflowgames.playgames

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.result.ActivityResult
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.games.GamesClientStatusCodes
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.android.gms.tasks.Tasks
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** PGS Saved Games (Snapshots). Conflicts resolve most-recently-modified-wins (no merge). */
internal class SavedGamesModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getSnapshotsClient(activity)

    fun load(call: PluginCall) {
        val name = call.getString("name") ?: return call.reject("missing name")
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
        val name = call.getString("name") ?: return call.reject("missing name")
        val data = call.getString("data") ?: return call.reject("missing data")
        val policy = call.conflictPolicyOption() ?: return
        val change = SnapshotMetadataChange.Builder()
            .setDescription(call.getString("description") ?: "")
        call.getLong("playedTimeMillis")?.let { change.setPlayedTimeMillis(it) }
        call.getLong("progressValue")?.let { change.setProgressValue(it) }
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
                snap.snapshotContents.writeBytes(data.toByteArray(StandardCharsets.UTF_8))
                client.commitAndClose(snap, metadataChange)
            }
            .bind(call, "snapshot save failed")
    }

    fun list(call: PluginCall) {
        val forceReload = call.getBoolean("forceReload", false) ?: false
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
        val name = call.getString("name") ?: return call.reject("missing name")
        client.load(false)
            .continueWithTask { task ->
                val meta = task.result?.get()?.use { buf ->
                    buf.firstOrNull { it.uniqueName == name }?.freeze()
                } ?: throw NoSuchElementException("snapshot '$name' not found")
                client.delete(meta)
            }
            .bind(call, "snapshot delete failed")
    }

    fun show(call: PluginCall) {
        val title = call.getString("title") ?: DEFAULT_PICKER_TITLE
        val allowAdd = call.getBoolean("allowAdd", true) ?: true
        val allowDelete = call.getBoolean("allowDelete", true) ?: true
        val maxSnapshots = call.getInt("maxSnapshots", SnapshotsClient.DISPLAY_LIMIT_NONE)
            ?: SnapshotsClient.DISPLAY_LIMIT_NONE
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
