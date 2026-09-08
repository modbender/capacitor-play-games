package com.idleflowgames.playgames

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/** PGS Saved Games (Snapshots). Conflicts resolve most-recently-modified-wins (no merge). */
internal class SavedGamesModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getSnapshotsClient(activity)

    fun load(call: PluginCall) {
        val name = call.getString("name") ?: return call.reject("missing name")
        // Blocking snapshot I/O off the main thread (GMS Task listeners default to UI).
        client.open(name, /* createIfNotFound = */ true, AUTO_RESOLVE)
            .addOnSuccessListener(IO_EXECUTOR) { result ->
                val snap = result.data
                if (snap == null) {
                    call.resolve(jsObject { put("snapshot", JSObject.NULL) })
                    return@addOnSuccessListener
                }
                val bytes = snap.snapshotContents.readFully() ?: ByteArray(0)
                val payload = jsObject {
                    put("name", snap.metadata.uniqueName)
                    put("description", snap.metadata.description ?: "")
                    put("modifiedAt", snap.metadata.lastModifiedTimestamp)
                    put("data", String(bytes, StandardCharsets.UTF_8))
                }
                client.discardAndClose(snap)
                call.resolve(jsObject { put("snapshot", payload) })
            }
            .addOnFailureListener(IO_EXECUTOR) { e ->
                call.rejectFromException(e, "snapshot load failed")
            }
    }

    fun save(call: PluginCall) {
        val name = call.getString("name") ?: return call.reject("missing name")
        val data = call.getString("data") ?: return call.reject("missing data")
        val description = call.getString("description") ?: ""

        // Blocking snapshot I/O off the main thread.
        client.open(name, /* createIfNotFound = */ true, AUTO_RESOLVE)
            .continueWithTask(IO_EXECUTOR) { task ->
                val snap = task.result?.data
                    ?: throw IllegalStateException("snapshot unavailable")
                snap.snapshotContents.writeBytes(data.toByteArray(StandardCharsets.UTF_8))
                val change = SnapshotMetadataChange.Builder()
                    .setDescription(description)
                    .build()
                client.commitAndClose(snap, change)
            }
            .bind(call, "snapshot save failed")
    }

    fun list(call: PluginCall) {
        client.load(/* forceReload = */ false).bind(call, "snapshot list failed") { result ->
            val arr = JSArray()
            result.get()?.use { buf ->
                for (i in 0 until buf.count) arr.put(buf.get(i).toJsObject())
            }
            jsObject { put("snapshots", arr) }
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

    private companion object {
        const val AUTO_RESOLVE = SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED

        // Serial daemon thread for snapshot file I/O.
        val IO_EXECUTOR: Executor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "pgs-saved-games").apply { isDaemon = true }
        }
    }
}

private fun SnapshotMetadata.toJsObject(): JSObject = jsObject {
    put("name", uniqueName)
    put("description", description ?: "")
    put("modifiedAt", lastModifiedTimestamp)
}

/** SnapshotMetadataBuffer has release() but isn't Closeable; provide a use {} of our own. */
private inline fun <R> com.google.android.gms.games.snapshot.SnapshotMetadataBuffer.use(
    block: (com.google.android.gms.games.snapshot.SnapshotMetadataBuffer) -> R,
): R {
    try {
        return block(this)
    } finally {
        release()
    }
}

private fun com.google.android.gms.games.snapshot.SnapshotMetadataBuffer.firstOrNull(
    predicate: (SnapshotMetadata) -> Boolean,
): SnapshotMetadata? {
    for (i in 0 until count) {
        val m = get(i)
        if (predicate(m)) return m
    }
    return null
}
