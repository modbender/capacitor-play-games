package com.idleflowgames.playgames

import com.getcapacitor.PluginCall
import com.google.android.gms.games.PlayGames

internal class RecallModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getRecallClient(activity)

    fun requestAccess(call: PluginCall) {
        client.requestRecallAccess().bind(call, "recall access failed") { access ->
            jsObject { put("sessionId", access.sessionId) }
        }
    }
}
