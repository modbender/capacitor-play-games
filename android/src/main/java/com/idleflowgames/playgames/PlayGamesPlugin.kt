package com.idleflowgames.playgames

import android.content.Intent
import androidx.activity.result.ActivityResult
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.tasks.Task

/**
 * Capacitor 8 plugin wrapping Google Play Games Services v2. Thin dispatcher to
 * the per-feature modules; the `@ActivityCallback` UI-result handlers live here
 * because Capacitor requires them on the Plugin subclass.
 */
@CapacitorPlugin(name = "PlayGames")
class PlayGamesPlugin : Plugin() {
    private val signIn by lazy { SignInModule(this) }
    private val achievements by lazy { AchievementsModule(this) }
    private val leaderboards by lazy { LeaderboardsModule(this) }
    private val savedGames by lazy { SavedGamesModule(this) }

    override fun load() {
        super.load()
        // Non-fatal: without Play Services, PGS calls resolve signedIn=false rather than crash.
        runCatching { PlayGamesSdk.initialize(context) }
    }

    /** Expose listener emission to modules; `notifyListeners` is protected. */
    internal fun emit(event: String, data: JSObject) =
        notifyListeners(event, data)

    /** Launch a PGS UI intent (achievements / leaderboards) via the activity-result bridge. */
    internal fun launchUiIntent(
        intentTask: Task<Intent>,
        call: PluginCall,
        callbackName: String,
    ) {
        intentTask
            .addOnSuccessListener { intent ->
                startActivityForResult(call, intent, callbackName)
            }
            .addOnFailureListener { e ->
                call.rejectFromException(e, "PGS intent failed")
            }
    }

    // ---- @PluginMethod entrypoints (JS-callable surface) -------------------

    @PluginMethod fun initialize(call: PluginCall) = call.resolve()

    @PluginMethod fun signIn(call: PluginCall) = signIn.signIn(call)
    @PluginMethod fun isSignedIn(call: PluginCall) = signIn.isSignedIn(call)
    @PluginMethod fun getPlayer(call: PluginCall) = signIn.getPlayer(call)
    @PluginMethod fun requestServerSideAccess(call: PluginCall) = signIn.requestServerSideAccess(call)

    @PluginMethod fun unlockAchievement(call: PluginCall) = achievements.unlock(call)
    @PluginMethod fun incrementAchievement(call: PluginCall) = achievements.increment(call)
    @PluginMethod fun showAchievements(call: PluginCall) = achievements.show(call)

    @PluginMethod fun submitScore(call: PluginCall) = leaderboards.submit(call)
    @PluginMethod fun showLeaderboard(call: PluginCall) = leaderboards.show(call)
    @PluginMethod fun showAllLeaderboards(call: PluginCall) = leaderboards.showAll(call)

    @PluginMethod fun loadSnapshot(call: PluginCall) = savedGames.load(call)
    @PluginMethod fun saveSnapshot(call: PluginCall) = savedGames.save(call)
    @PluginMethod fun listSnapshots(call: PluginCall) = savedGames.list(call)
    @PluginMethod fun deleteSnapshot(call: PluginCall) = savedGames.delete(call)

    // ---- @ActivityCallback handlers ----------------------------------------

    @Suppress("unused") // referenced by name from launchUiIntent
    @ActivityCallback
    private fun onAchievementsUiResult(call: PluginCall?, result: ActivityResult) =
        resolveUiResult(call, result)

    @Suppress("unused")
    @ActivityCallback
    private fun onLeaderboardUiResult(call: PluginCall?, result: ActivityResult) =
        resolveUiResult(call, result)

    private fun resolveUiResult(call: PluginCall?, @Suppress("UNUSED_PARAMETER") result: ActivityResult) {
        call?.resolve()
    }
}
