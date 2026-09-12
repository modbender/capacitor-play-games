package com.idleflowgames.playgames

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
    private val gameStats by lazy { GameStatsModule(this) }
    private val events by lazy { EventsModule(this) }
    private val recall by lazy { RecallModule(this) }
    private val playerStats by lazy { PlayerStatsModule(this) }

    internal val players by lazy { PlayersModule(this) }

    private var intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var intentSenderCallback: ((Boolean) -> Unit)? = null

    override fun load() {
        super.load()
        // Non-fatal: without Play Services, PGS calls resolve signedIn=false rather than crash.
        runCatching { PlayGamesSdk.initialize(context) }
        // The friends-list consent flow arrives as a PendingIntent, which Capacitor's
        // Intent-only startActivityForResult cannot carry. Registering has to happen
        // during load, alongside Capacitor's own launchers, because the host activity
        // refuses new registrations once it has started.
        intentSenderLauncher = runCatching {
            bridge.registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult(),
            ) { result ->
                val callback = intentSenderCallback
                intentSenderCallback = null
                callback?.invoke(result.resultCode == Activity.RESULT_OK)
            }
        }.getOrNull()
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

    /**
     * Launch a GMS resolution PendingIntent. False means nothing was launched, whether
     * because no launcher is registered, because another resolution is still outstanding,
     * or because the launch threw; the caller reports the unresolved state instead.
     */
    internal fun launchIntentSender(
        pendingIntent: PendingIntent,
        onResult: (Boolean) -> Unit,
    ): Boolean {
        val launcher = intentSenderLauncher ?: return false
        if (intentSenderCallback != null) return false
        intentSenderCallback = onResult
        return runCatching {
            launcher.launch(IntentSenderRequest.Builder(pendingIntent).build())
        }.isSuccess.also { launched -> if (!launched) intentSenderCallback = null }
    }

    // ---- @PluginMethod entrypoints (JS-callable surface) -------------------

    @PluginMethod fun initialize(call: PluginCall) = call.resolve()

    @PluginMethod fun signIn(call: PluginCall) = signIn.signIn(call)
    @PluginMethod fun isSignedIn(call: PluginCall) = signIn.isSignedIn(call)
    @PluginMethod fun requestServerSideAccess(call: PluginCall) = signIn.requestServerSideAccess(call)

    @PluginMethod fun getPlayer(call: PluginCall) = players.getPlayer(call)
    @PluginMethod fun getPlayerId(call: PluginCall) = players.getPlayerId(call)
    @PluginMethod fun loadPlayer(call: PluginCall) = players.loadPlayer(call)
    @PluginMethod fun loadFriends(call: PluginCall) = players.loadFriends(call)
    @PluginMethod fun loadRecentlyPlayedWithPlayers(call: PluginCall) = players.loadRecentlyPlayedWith(call)
    @PluginMethod fun showPlayerSearch(call: PluginCall) = players.showSearch(call)
    @PluginMethod fun showComparePlayer(call: PluginCall) = players.showCompare(call)

    @PluginMethod fun unlockAchievement(call: PluginCall) = achievements.unlock(call)
    @PluginMethod fun revealAchievement(call: PluginCall) = achievements.reveal(call)
    @PluginMethod fun incrementAchievement(call: PluginCall) = achievements.increment(call)
    @PluginMethod fun setAchievementSteps(call: PluginCall) = achievements.setSteps(call)
    @PluginMethod fun loadAchievements(call: PluginCall) = achievements.load(call)
    @PluginMethod fun showAchievements(call: PluginCall) = achievements.show(call)

    @PluginMethod fun submitScore(call: PluginCall) = leaderboards.submit(call)
    @PluginMethod fun showLeaderboard(call: PluginCall) = leaderboards.show(call)
    @PluginMethod fun showAllLeaderboards(call: PluginCall) = leaderboards.showAll(call)
    @PluginMethod fun loadLeaderboards(call: PluginCall) = leaderboards.loadAll(call)
    @PluginMethod fun loadLeaderboard(call: PluginCall) = leaderboards.loadOne(call)
    @PluginMethod fun loadTopScores(call: PluginCall) = leaderboards.loadTopScores(call)
    @PluginMethod fun loadPlayerCenteredScores(call: PluginCall) = leaderboards.loadPlayerCenteredScores(call)
    @PluginMethod fun loadCurrentPlayerScore(call: PluginCall) = leaderboards.loadCurrentPlayerScore(call)

    @PluginMethod fun loadSnapshot(call: PluginCall) = savedGames.load(call)
    @PluginMethod fun saveSnapshot(call: PluginCall) = savedGames.save(call)
    @PluginMethod fun listSnapshots(call: PluginCall) = savedGames.list(call)
    @PluginMethod fun deleteSnapshot(call: PluginCall) = savedGames.delete(call)
    @PluginMethod fun showSnapshots(call: PluginCall) = savedGames.show(call)
    @PluginMethod fun getSnapshotLimits(call: PluginCall) = savedGames.limits(call)

    @PluginMethod fun recordGameEvent(call: PluginCall) = gameStats.recordEvent(call)
    @PluginMethod fun recordGameEvents(call: PluginCall) = gameStats.recordEvents(call)
    @PluginMethod fun recordProgressUpdate(call: PluginCall) = gameStats.recordProgressUpdate(call)
    @PluginMethod fun requestGameEventsUpload(call: PluginCall) = gameStats.requestUpload(call)

    @PluginMethod fun incrementEvent(call: PluginCall) = events.increment(call)
    @PluginMethod fun loadEvents(call: PluginCall) = events.load(call)
    @PluginMethod fun loadEventsByIds(call: PluginCall) = events.loadByIds(call)

    @PluginMethod fun requestRecallAccess(call: PluginCall) = recall.requestAccess(call)

    @PluginMethod fun loadPlayerStats(call: PluginCall) = playerStats.load(call)

    // ---- @ActivityCallback handlers ----------------------------------------

    @Suppress("unused") // referenced by name from launchUiIntent
    @ActivityCallback
    private fun onAchievementsUiResult(call: PluginCall?, result: ActivityResult) =
        resolveUiResult(call, result)

    @Suppress("unused")
    @ActivityCallback
    private fun onLeaderboardUiResult(call: PluginCall?, result: ActivityResult) =
        resolveUiResult(call, result)

    @Suppress("unused")
    @ActivityCallback
    private fun onComparePlayerUiResult(call: PluginCall?, result: ActivityResult) =
        resolveUiResult(call, result)

    @Suppress("unused")
    @ActivityCallback
    private fun onPlayerSearchUiResult(call: PluginCall?, result: ActivityResult) {
        if (call != null) players.resolveSearchResult(call, result)
    }

    @Suppress("unused")
    @ActivityCallback
    private fun onSnapshotUiResult(call: PluginCall?, result: ActivityResult) {
        if (call != null) savedGames.resolveSelection(call, result)
    }

    private fun resolveUiResult(call: PluginCall?, @Suppress("UNUSED_PARAMETER") result: ActivityResult) {
        call?.resolve()
    }
}
