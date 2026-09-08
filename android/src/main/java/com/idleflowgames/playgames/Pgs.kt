package com.idleflowgames.playgames

import androidx.appcompat.app.AppCompatActivity
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.games.Player
import com.google.android.gms.tasks.Task

/** Shared base for the per-feature modules; carries the plugin back-reference. */
internal abstract class PgsModule(protected val plugin: PlayGamesPlugin) {
    protected val activity: AppCompatActivity
        get() = plugin.activity
}

/** Build a JSObject with a fluent block — `jsObject { put("k", v) }`. */
internal inline fun jsObject(builder: JSObject.() -> Unit): JSObject =
    JSObject().apply(builder)

/** Reject a PluginCall, exposing a GMS ApiException statusCode as the Capacitor error code. */
internal fun PluginCall.rejectFromException(e: Exception, fallbackMsg: String) {
    val message = e.message ?: fallbackMsg
    val status = (e as? ApiException)?.statusCode
    if (status != null) reject(message, status.toString(), e) else reject(message, e)
}

/** Resolve a void-yielding Task to a PluginCall (success resolves, failure rejects). */
internal fun Task<*>.bind(call: PluginCall, errorMsg: String = "operation failed") {
    addOnSuccessListener { call.resolve() }
    addOnFailureListener { e -> call.rejectFromException(e, errorMsg) }
}

/** Resolve a value-yielding Task to a PluginCall, mapping the result to a JSObject. */
internal inline fun <T> Task<T>.bind(
    call: PluginCall,
    errorMsg: String = "operation failed",
    crossinline transform: (T) -> JSObject,
) {
    addOnSuccessListener { result -> call.resolve(transform(result)) }
    addOnFailureListener { e -> call.rejectFromException(e, errorMsg) }
}

/** Serialise a PGS Player as the JS-side `PlayerInfo` shape. */
internal fun Player.toJsObject(): JSObject = jsObject {
    put("playerId", playerId)
    put("displayName", displayName ?: "")
    iconImageUri?.toString()?.let { put("avatarUrl", it) }
}
