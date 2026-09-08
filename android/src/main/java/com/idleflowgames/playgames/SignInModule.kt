package com.idleflowgames.playgames

import com.getcapacitor.PluginCall
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.games.PlayGames

internal class SignInModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val signInClient get() = PlayGames.getGamesSignInClient(activity)
    private val playersClient get() = PlayGames.getPlayersClient(activity)

    fun signIn(call: PluginCall) {
        val silent = call.getBoolean("silent", true) ?: true
        // Interactive sign-in can crash uncatchably inside GMS's
        // GamesResolutionActivity on devices with a broken Play Services install
        // (some custom ROMs). Pre-flight the availability check and bail to
        // signed-out rather than launch the activity. Silent auth shows no UI.
        if (!silent) {
            val gmsStatus = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(activity)
            if (gmsStatus != ConnectionResult.SUCCESS) {
                resolveSignedOut(call)
                return
            }
        }
        val task = if (silent) signInClient.isAuthenticated else signInClient.signIn()
        task
            .addOnSuccessListener { result ->
                if (result.isAuthenticated) resolveWithPlayer(call) else resolveSignedOut(call)
            }
            .addOnFailureListener {
                resolveSignedOut(call)
            }
    }

    fun isSignedIn(call: PluginCall) {
        signInClient.isAuthenticated.bind(call, "auth check failed") { result ->
            jsObject { put("signedIn", result.isAuthenticated) }
        }
    }

    fun requestServerSideAccess(call: PluginCall) {
        val serverClientId = call.getString("serverClientId")
        if (serverClientId.isNullOrEmpty()) {
            call.reject("serverClientId is required")
            return
        }
        val forceRefresh = call.getBoolean("forceRefresh", false) ?: false
        signInClient.requestServerSideAccess(serverClientId, forceRefresh)
            .bind(call, "server-side access failed") { authCode ->
                jsObject { put("authCode", authCode) }
            }
    }

    fun getPlayer(call: PluginCall) {
        playersClient.currentPlayer.bind(call, "player lookup failed") { player ->
            player.toJsObject()
        }
    }

    private fun resolveWithPlayer(call: PluginCall) {
        playersClient.currentPlayer
            .addOnSuccessListener { player ->
                emitAndResolve(call, jsObject {
                    put("signedIn", true)
                    put("player", player.toJsObject())
                })
            }
            .addOnFailureListener {
                // Signed in but profile lookup failed: report signed-in, no profile.
                emitAndResolve(call, jsObject { put("signedIn", true) })
            }
    }

    private fun resolveSignedOut(call: PluginCall) {
        emitAndResolve(call, jsObject { put("signedIn", false) })
    }

    private fun emitAndResolve(call: PluginCall, body: com.getcapacitor.JSObject) {
        plugin.emit("signInStateChanged", body)
        call.resolve(body)
    }
}
