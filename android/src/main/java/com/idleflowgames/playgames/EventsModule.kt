package com.idleflowgames.playgames

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.event.Event
import com.google.android.gms.games.event.EventBuffer
import org.json.JSONException

/** The legacy PGS events feature, which is not the same thing as game stats. */
internal class EventsModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getEventsClient(activity)

    fun increment(call: PluginCall) {
        val eventId = call.getString("eventId") ?: return call.reject("missing eventId")
        val amount = call.getInt("amount") ?: return call.reject("missing amount")
        client.increment(eventId, amount)
        call.resolve()
    }

    fun load(call: PluginCall) {
        val forceReload = call.getBoolean("forceReload", false) ?: false
        client.load(forceReload).bindAnnotated(call, "loadEvents failed", ::eventsPayload)
    }

    fun loadByIds(call: PluginCall) {
        val eventIds = try {
            call.getArray("eventIds")?.toList<String>()
        } catch (e: JSONException) {
            return call.reject("eventIds must be an array of strings")
        } ?: return call.reject("missing eventIds")
        if (eventIds.isEmpty()) return call.reject("eventIds must not be empty")
        val forceReload = call.getBoolean("forceReload", false) ?: false
        client.loadByIds(forceReload, *eventIds.toTypedArray())
            .bindAnnotated(call, "loadEventsByIds failed", ::eventsPayload)
    }
}

private fun eventsPayload(buffer: EventBuffer?): JSObject = jsObject {
    put("events", buffer?.use { it.toJsArray(Event::toJsObject) } ?: JSArray())
}

internal fun Event.toJsObject(): JSObject = jsObject {
    put("eventId", eventId)
    put("name", name.orEmpty())
    put("description", description.orEmpty())
    putIfPresent("iconImageUrl", iconImageUri)
    put("value", value)
    put("formattedValue", formattedValue.orEmpty())
    put("visible", isVisible)
}
