package com.idleflowgames.playgames

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Releasable
import com.google.android.gms.games.AnnotatedData
import com.google.android.gms.games.Player
import com.google.android.gms.games.PlayerLevel
import com.google.android.gms.games.PlayerLevelInfo
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

/**
 * Resolve an AnnotatedData-yielding Task, adding the SDK's staleness flag to the
 * result. The payload is nullable because `AnnotatedData.get()` is.
 */
internal inline fun <D> Task<AnnotatedData<D>>.bindAnnotated(
    call: PluginCall,
    errorMsg: String = "operation failed",
    crossinline transform: (D?) -> JSObject,
) {
    bind(call, errorMsg) { annotated ->
        transform(annotated.get()).apply { put("stale", annotated.isStale) }
    }
}

/**
 * GMS buffers and `LeaderboardsClient.LeaderboardScores` hold native memory behind
 * `Releasable`; a missed release leaks silently. Anything read out of one must be
 * serialised inside the block, because the entries are cursor views that go stale
 * the moment `release()` runs.
 */
internal inline fun <B : Releasable, R> B.use(block: (B) -> R): R {
    try {
        return block(this)
    } finally {
        release()
    }
}

internal inline fun <T> Iterable<T>.toJsArray(transform: (T) -> Any?): JSArray =
    JSArray().also { arr -> for (item in this) transform(item)?.let(arr::put) }

/**
 * The SDK signals "no value" with a -1 sentinel on several numeric getters. JS wants
 * those as `undefined`, and omitting the key is what produces that.
 */
internal fun JSObject.putUnlessSentinel(key: String, value: Long, sentinel: Long) {
    if (value != sentinel) put(key, value)
}

internal fun JSObject.putUnlessSentinel(key: String, value: Int, sentinel: Int) {
    if (value != sentinel) put(key, value)
}

internal fun JSObject.putUnlessSentinel(key: String, value: Float, sentinel: Float) {
    if (value != sentinel) put(key, value.toDouble())
}

internal fun JSObject.putIfPresent(key: String, value: String?) {
    if (!value.isNullOrEmpty()) put(key, value)
}

internal fun JSObject.putIfPresent(key: String, value: Uri?) {
    if (value != null) put(key, value.toString())
}

internal fun JSObject.putIfPresent(key: String, value: JSObject?) {
    if (value != null) put(key, value)
}

/**
 * Serialise a PGS Player as the JS-side `PlayerInfo` shape. `lastPlayedWithTimestamp`
 * is deprecated upstream; the 0.5.0 surface still carries it.
 */
@Suppress("DEPRECATION")
internal fun Player.toJsObject(): JSObject = jsObject {
    put("playerId", playerId)
    put("displayName", displayName.orEmpty())
    if (hasIconImage()) putIfPresent("avatarUrl", iconImageUri)
    if (hasHiResImage()) putIfPresent("hiResImageUrl", hiResImageUri)
    putIfPresent("bannerImageLandscapeUrl", bannerImageLandscapeUri)
    putIfPresent("bannerImagePortraitUrl", bannerImagePortraitUri)
    putIfPresent("title", title)
    putUnlessSentinel("retrievedAt", retrievedTimestamp, Player.TIMESTAMP_UNKNOWN)
    putUnlessSentinel("lastPlayedWithAt", lastPlayedWithTimestamp, Player.TIMESTAMP_UNKNOWN)
    putIfPresent("level", levelInfo?.toJsObject())
    putIfPresent("friendStatus", relationshipInfo?.friendStatus?.let(FRIEND_STATUSES::nameOf))
    putIfPresent(
        "friendsListVisibility",
        currentPlayerInfo?.friendsListVisibilityStatus?.let(FRIENDS_LIST_VISIBILITIES::nameOf),
    )
}

internal fun PlayerLevelInfo.toJsObject(): JSObject = jsObject {
    putUnlessSentinel("currentXpTotal", currentXpTotal, Player.CURRENT_XP_UNKNOWN)
    putUnlessSentinel("lastLevelUpAt", lastLevelUpTimestamp, Player.TIMESTAMP_UNKNOWN)
    put("isMaxLevel", isMaxLevel)
    put("currentLevel", currentLevel.toJsObject())
    put("nextLevel", nextLevel.toJsObject())
}

internal fun PlayerLevel.toJsObject(): JSObject = jsObject {
    put("levelNumber", levelNumber)
    put("minXp", minXp)
    put("maxXp", maxXp)
}
