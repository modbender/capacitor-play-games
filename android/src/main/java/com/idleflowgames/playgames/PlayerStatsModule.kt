package com.idleflowgames.playgames

import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.stats.PlayerStats

internal class PlayerStatsModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getPlayerStatsClient(activity)

    fun load(call: PluginCall) {
        val forceReload = call.getBoolean("forceReload", false) ?: false
        client.loadPlayerStats(forceReload).bindAnnotated(call, "loadPlayerStats failed") { stats ->
            jsObject { put("stats", stats?.toJsObject() ?: JSObject()) }
        }
    }
}

/** The three count getters are ints; the SDK declares only the float form of the sentinel. */
private val UNSET_COUNT = PlayerStats.UNSET_VALUE.toInt()

@Suppress("DEPRECATION") // several probability fields are deprecated upstream and still bound
internal fun PlayerStats.toJsObject(): JSObject = jsObject {
    putUnlessSentinel("averageSessionLength", averageSessionLength, PlayerStats.UNSET_VALUE)
    putUnlessSentinel("churnProbability", churnProbability, PlayerStats.UNSET_VALUE)
    putUnlessSentinel("daysSinceLastPlayed", daysSinceLastPlayed, UNSET_COUNT)
    putUnlessSentinel("numberOfPurchases", numberOfPurchases, UNSET_COUNT)
    putUnlessSentinel("numberOfSessions", numberOfSessions, UNSET_COUNT)
    putUnlessSentinel("sessionPercentile", sessionPercentile, PlayerStats.UNSET_VALUE)
    putUnlessSentinel("spendPercentile", spendPercentile, PlayerStats.UNSET_VALUE)
    putUnlessSentinel("spendProbability", spendProbability, PlayerStats.UNSET_VALUE)
    putUnlessSentinel("highSpenderProbability", highSpenderProbability, PlayerStats.UNSET_VALUE)
    putUnlessSentinel("totalSpendNext28Days", totalSpendNext28Days, PlayerStats.UNSET_VALUE)
}
