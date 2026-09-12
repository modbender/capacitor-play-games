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

// ---- Call option readers -------------------------------------------------

/**
 * The raw value the call carries for `key`; absent and explicitly null both read as
 * null here. Every typed getter answers a wrong-typed option with the same default it
 * answers an absent one with, so this is what separates the two.
 */
internal fun PluginCall.rawOption(key: String): Any? = if (data.isNull(key)) null else data.opt(key)

/**
 * Read a numeric option out of the raw value. The typed getters match on the boxed
 * class: `getLong` takes only a `Long`, `getInt` only an `Integer`, `getDouble` anything
 * but a `Long`. org.json boxes a JSON integer as `Integer` or `Long` by its magnitude, so
 * each of them falls back to its default over part of the range a JS number carries.
 */
internal fun PluginCall.numberOption(key: String): Number? = rawOption(key) as? Number

/** Reject an option that is absent, or present with a type it cannot be read as. */
private fun PluginCall.rejectOption(key: String, expected: String): Nothing? {
    if (rawOption(key) == null) reject("missing $key") else reject("$key must be $expected")
    return null
}

/** Read a required string option; rejects when it is absent or is not a string. */
internal fun PluginCall.requireString(key: String): String? =
    getString(key) ?: rejectOption(key, "a string")

/** Read a required array option; rejects when it is absent or is not an array. */
internal fun PluginCall.requireArray(key: String): JSArray? =
    getArray(key) ?: rejectOption(key, "an array")

/** Read a required numeric option; rejects when it is absent or is not a number. */
internal fun PluginCall.requireNumber(key: String): Number? =
    numberOption(key) ?: rejectOption(key, "a number")

/** Read a required option the SDK takes as an `int`. */
internal fun PluginCall.requireInt(key: String): Int? = (requireNumber(key) ?: return null).toInt32(this, key)

/** Read an option the SDK takes as an `int`, falling back to `default` when absent. */
internal fun PluginCall.intOption(key: String, default: Int): Int? {
    if (rawOption(key) == null) return default
    return requireInt(key)
}

/**
 * Set an optional field the SDK takes as a `long`. An absent option leaves the field
 * alone; a present one that is not a number rejects the call and returns false, because
 * dropping it would resolve the call as a success with the field unset.
 */
internal fun PluginCall.readLong(key: String, set: (Long) -> Unit): Boolean {
    if (rawOption(key) == null) return true
    val value = requireNumber(key) ?: return false
    set(value.toLong())
    return true
}

private fun Number.toInt32(call: PluginCall, key: String): Int? {
    val wide = toLong()
    if (wide < Int.MIN_VALUE || wide > Int.MAX_VALUE) {
        call.reject("$key must fit in a 32-bit integer, got $wide")
        return null
    }
    return wide.toInt()
}

/**
 * Read a boolean option, falling back to `default` when absent.
 *
 * Lenient where the `require*` readers are strict: a present but wrong-typed value
 * also takes the default rather than rejecting. A bail channel would mean a nullable
 * return threaded through sixteen call sites, and the failure is mild next to a
 * dropped score or playtime. Fourteen of the sixteen default toward less work or less
 * UI, so a wrong type errs quiet. The exception is the snapshot picker's `allowAdd`
 * and `allowDelete`, which default true, so a wrong-typed value offers an affordance
 * the caller meant to withhold.
 */
internal fun PluginCall.boolOption(key: String, default: Boolean): Boolean =
    getBoolean(key, default) ?: default

/** The reload flag every cached PGS read takes. */
internal fun PluginCall.forceReload(): Boolean = boolOption("forceReload", false)

/**
 * Read an array option as a list of strings. `JSArray.toList` casts unchecked, so a
 * non-string element surfaces wherever the list is consumed rather than here, and an
 * exception out of a @PluginMethod reaches the bridge as a process kill rather than a
 * rejected call.
 */
internal fun PluginCall.requireStringList(key: String): List<String>? {
    val array = requireArray(key) ?: return null
    val values = ArrayList<String>(array.length())
    for (index in 0 until array.length()) {
        val element = array.opt(index)
        if (element !is String) {
            reject("$key[$index] must be a string")
            return null
        }
        values.add(element)
    }
    return values
}

/**
 * Serialise a PGS Player as the JS-side `PlayerInfo` shape. `lastPlayedWithTimestamp`
 * is deprecated upstream; the bridge still carries it.
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
