package com.idleflowgames.playgames

import com.getcapacitor.PluginCall
import com.google.android.gms.games.PlayGames

internal class LeaderboardsModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getLeaderboardsClient(activity)

    fun submit(call: PluginCall) {
        val id = call.getString("leaderboardId") ?: return call.reject("missing leaderboardId")
        val score = call.getDouble("score") ?: return call.reject("missing score")
        client.submitScoreImmediate(id, score.toLong())
            .addOnSuccessListener { call.resolve() }
            .addOnFailureListener { e ->
                call.rejectFromException(e, "submitScore failed")
            }
    }

    fun show(call: PluginCall) {
        val id = call.getString("leaderboardId") ?: return call.reject("missing leaderboardId")
        plugin.launchUiIntent(client.getLeaderboardIntent(id), call, "onLeaderboardUiResult")
    }

    fun showAll(call: PluginCall) {
        plugin.launchUiIntent(client.allLeaderboardsIntent, call, "onLeaderboardUiResult")
    }
}
