package com.idleflowgames.playgames

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.achievement.Achievement

internal class AchievementsModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getAchievementsClient(activity)

    fun unlock(call: PluginCall) {
        val id = call.getString("id") ?: return call.reject("missing id")
        client.unlockImmediate(id).bind(call, "unlockAchievement failed")
    }

    fun reveal(call: PluginCall) {
        val id = call.getString("id") ?: return call.reject("missing id")
        client.revealImmediate(id).bind(call, "revealAchievement failed")
    }

    fun increment(call: PluginCall) {
        val id = call.getString("id") ?: return call.reject("missing id")
        val steps = call.stepsOption() ?: return
        client.incrementImmediate(id, steps).bind(call, "incrementAchievement failed") { unlocked ->
            jsObject { put("unlocked", unlocked) }
        }
    }

    fun setSteps(call: PluginCall) {
        val id = call.getString("id") ?: return call.reject("missing id")
        val steps = call.stepsOption() ?: return
        client.setStepsImmediate(id, steps).bind(call, "setAchievementSteps failed") { unlocked ->
            jsObject { put("unlocked", unlocked) }
        }
    }

    fun load(call: PluginCall) {
        val forceReload = call.getBoolean("forceReload", false) ?: false
        client.load(forceReload).bindAnnotated(call, "loadAchievements failed") { buffer ->
            jsObject {
                put("achievements", buffer?.use { it.toJsArray(Achievement::toJsObject) } ?: JSArray())
            }
        }
    }

    fun show(call: PluginCall) {
        plugin.launchUiIntent(client.achievementsIntent, call, "onAchievementsUiResult")
    }
}

private fun PluginCall.stepsOption(): Int? {
    val steps = getInt("steps")
    if (steps == null) {
        reject("missing steps")
        return null
    }
    if (steps <= 0) {
        reject("steps must be > 0")
        return null
    }
    return steps
}

internal fun Achievement.toJsObject(): JSObject? {
    val typeName = ACHIEVEMENT_TYPES.nameOf(type) ?: return null
    val stateName = ACHIEVEMENT_STATES.nameOf(state) ?: return null
    return jsObject {
        put("id", achievementId)
        put("type", typeName)
        put("name", name.orEmpty())
        put("description", description.orEmpty())
        put("state", stateName)
        put("xpValue", xpValue)
        put("lastUpdatedAt", lastUpdatedTimestamp)
        putIfPresent("unlockedImageUrl", unlockedImageUri)
        putIfPresent("revealedImageUrl", revealedImageUri)
        // The step getters throw outright on a standard achievement.
        if (type == Achievement.TYPE_INCREMENTAL) {
            put("currentSteps", currentSteps)
            put("totalSteps", totalSteps)
            putIfPresent("formattedCurrentSteps", formattedCurrentSteps)
            putIfPresent("formattedTotalSteps", formattedTotalSteps)
        }
    }
}
