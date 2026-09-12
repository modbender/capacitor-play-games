package com.idleflowgames.playgames

import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.playergameevent.PlayerGameEvent
import org.json.JSONException
import org.json.JSONObject

/**
 * Play Games game stats. All three client methods return void, so a resolved call
 * means the event was handed to Play Services, not that it reached Google.
 */
internal class GameStatsModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getGameStatsClient(activity)

    fun recordEvent(call: PluginCall) {
        val name = call.requireString("name") ?: return
        val event = call.buildOrReject { buildEvent(name, call.getObject("properties")) } ?: return
        client.recordEvent(event)
        call.resolve()
    }

    fun recordEvents(call: PluginCall) {
        val raw = call.requireArray("events") ?: return
        if (raw.length() > MAX_EVENTS_PER_BATCH) {
            return call.reject("at most $MAX_EVENTS_PER_BATCH events per batch, got ${raw.length()}")
        }
        val events = call.buildOrReject {
            (0 until raw.length()).map { index ->
                val source = raw.opt(index) as? JSONObject
                    ?: throw IllegalArgumentException("events[$index] is not an object")
                val entry = JSObject.fromJSONObject(source)
                val name = entry.getString("name")
                    ?: throw IllegalArgumentException("events[$index] is missing name")
                buildEvent(name, entry.getJSObject("properties"))
            }
        } ?: return
        if (events.isNotEmpty()) client.recordEvents(events)
        call.resolve()
    }

    fun recordProgressUpdate(call: PluginCall) {
        val currentProgress = call.requireInt("currentProgress") ?: return
        val event = call.buildOrReject {
            buildEvent(
                PROGRESS_UPDATE_EVENT_NAME,
                call.getObject("properties"),
                reserved = mapOf(PROGRESS_UPDATE_PROPERTY to currentProgress.toLong()),
            )
        } ?: return
        client.recordEvent(event)
        call.resolve()
    }

    fun requestUpload(call: PluginCall) {
        client.requestEventsUpload()
        call.resolve()
    }
}

private const val MAX_EVENTS_PER_BATCH = 30
private const val MAX_PROPERTIES_PER_EVENT = 25
private const val MAX_EVENT_NAME_LENGTH = 100
private const val MAX_PROPERTY_KEY_LENGTH = 100
private const val MAX_STRING_VALUE_LENGTH = 1024
private const val PROGRESS_UPDATE_EVENT_NAME = "progressUpdate"
private const val PROGRESS_UPDATE_PROPERTY = "currentProgress"

/**
 * The four tags are the whole property surface: they map one-to-one onto the four
 * `PlayerGameEvent.Builder.addProperty` overloads, and the declared type is what a
 * Play Console stat aggregates by, so it cannot be guessed from the JSON value.
 */
private val PROPERTY_BINDERS: Map<String, (PlayerGameEvent.Builder, String, JSObject) -> Unit> = mapOf(
    "int" to { builder, key, value -> builder.addProperty(key, value.getLong("int")) },
    "double" to { builder, key, value -> builder.addProperty(key, value.getDouble("double")) },
    "string" to { builder, key, value ->
        val text = value.getString("string").orEmpty()
        require(text.length <= MAX_STRING_VALUE_LENGTH) {
            "property '$key' string value is longer than $MAX_STRING_VALUE_LENGTH characters"
        }
        builder.addProperty(key, text)
    },
    "bool" to { builder, key, value -> builder.addProperty(key, value.getBoolean("bool")) },
)

/** Run an event build, turning a limit breach or a malformed payload into a reject. */
private inline fun <T> PluginCall.buildOrReject(build: () -> T): T? = try {
    build()
} catch (e: IllegalArgumentException) {
    reject(e.message ?: "invalid game event")
    null
} catch (e: JSONException) {
    reject("game event properties must be objects tagged ${PROPERTY_BINDERS.keys.joinToString()}")
    null
}

private fun buildEvent(
    name: String,
    properties: JSObject?,
    reserved: Map<String, Long> = emptyMap(),
): PlayerGameEvent {
    require(name.isNotEmpty()) { "event name must not be empty" }
    require(name.length <= MAX_EVENT_NAME_LENGTH) {
        "event name '$name' is longer than $MAX_EVENT_NAME_LENGTH characters"
    }
    val keys = properties?.keys()?.asSequence()?.toList().orEmpty()
    val total = keys.size + reserved.size
    require(total <= MAX_PROPERTIES_PER_EVENT) {
        "at most $MAX_PROPERTIES_PER_EVENT properties per event, got $total"
    }
    val builder = PlayerGameEvent.Builder(name)
    for (key in keys) {
        require(key.length <= MAX_PROPERTY_KEY_LENGTH) {
            "property key '$key' is longer than $MAX_PROPERTY_KEY_LENGTH characters"
        }
        val value = properties?.getJSObject(key)
            ?: throw IllegalArgumentException(
                "property '$key' must be an object tagged ${PROPERTY_BINDERS.keys.joinToString()}",
            )
        // The tag is erased by the time JSON reaches here, so TypeScript's union cannot
        // stop a caller sending two of them; whichever was read first would then decide
        // how the Play Console aggregates the stat.
        val tags = PROPERTY_BINDERS.keys.filter(value::has)
        require(tags.size == 1) {
            "property '$key' must carry exactly one of ${PROPERTY_BINDERS.keys.joinToString()}, " +
                if (tags.isEmpty()) "got none" else "got ${tags.joinToString()}"
        }
        PROPERTY_BINDERS.getValue(tags.first())(builder, key, value)
    }
    // Added last so a caller-supplied property of the same name cannot displace the
    // reserved one the Play Console stat is defined against.
    reserved.forEach { (key, value) -> builder.addProperty(key, value) }
    return builder.build()
}
