package com.idleflowgames.playgames

import com.getcapacitor.PluginCall
import com.google.android.gms.games.PlayGames

internal class AchievementsModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getAchievementsClient(activity)

    fun unlock(call: PluginCall) {
        val id = call.getString("id") ?: return call.reject("missing id")
        client.unlock(id)
        call.resolve()
    }

    fun increment(call: PluginCall) {
        val id = call.getString("id") ?: return call.reject("missing id")
        val steps = call.getInt("steps") ?: return call.reject("missing steps")
        if (steps <= 0) return call.reject("steps must be > 0")
        client.increment(id, steps)
        call.resolve()
    }

    fun show(call: PluginCall) {
        plugin.launchUiIntent(client.achievementsIntent, call, "onAchievementsUiResult")
    }
}
