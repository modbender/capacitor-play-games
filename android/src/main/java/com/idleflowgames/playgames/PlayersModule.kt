package com.idleflowgames.playgames

import androidx.activity.result.ActivityResult
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.games.FriendsResolutionRequiredException
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.Player
import com.google.android.gms.games.PlayerEntity
import com.google.android.gms.games.PlayersClient
import com.google.android.gms.tasks.Task

internal class PlayersModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getPlayersClient(activity)

    /** The signed-in profile, shared with SignInModule so only one client is reached for. */
    fun currentPlayer(): Task<Player> = client.currentPlayer

    fun getPlayer(call: PluginCall) {
        currentPlayer().bind(call, "player lookup failed") { it.toJsObject() }
    }

    fun getPlayerId(call: PluginCall) {
        client.currentPlayerId.bind(call, "player id lookup failed") { id ->
            jsObject { put("playerId", id) }
        }
    }

    fun loadPlayer(call: PluginCall) {
        val playerId = call.requireString("playerId") ?: return
        val forceReload = call.forceReload()
        client.loadPlayer(playerId, forceReload).bindAnnotated(call, "loadPlayer failed") { player ->
            jsObject { put("player", player?.toJsObject() ?: JSObject.NULL) }
        }
    }

    @Suppress("DEPRECATION") // deprecated upstream; bound deliberately rather than dropped
    fun loadRecentlyPlayedWith(call: PluginCall) {
        val pageSize = call.intOption("pageSize", DEFAULT_PAGE_SIZE) ?: return
        val forceReload = call.forceReload()
        client.loadRecentlyPlayedWithPlayers(pageSize, forceReload)
            .bindAnnotated(call, "loadRecentlyPlayedWithPlayers failed") { buffer ->
                jsObject {
                    put("players", buffer?.use { it.toJsArray(Player::toJsObject) } ?: JSArray())
                }
            }
    }

    fun loadFriends(call: PluginCall) {
        val pageSize = call.intOption("pageSize", DEFAULT_PAGE_SIZE) ?: return
        val forceReload = call.forceReload()
        val resolve = call.boolOption("resolve", false)
        loadFriendsPage(call, pageSize, forceReload, allowResolution = resolve)
    }

    fun showSearch(call: PluginCall) {
        plugin.launchUiIntent(client.playerSearchIntent, call, "onPlayerSearchUiResult")
    }

    fun showCompare(call: PluginCall) {
        val playerId = call.requireString("playerId") ?: return
        val otherName = call.getString("otherPlayerInGameName")
        val currentName = call.getString("currentPlayerInGameName")
        val intent = if (otherName == null && currentName == null) {
            client.getCompareProfileIntent(playerId)
        } else {
            client.getCompareProfileIntentWithAlternativeNameHints(playerId, otherName, currentName)
        }
        plugin.launchUiIntent(intent, call, "onComparePlayerUiResult")
    }

    /** Resolve a `showPlayerSearch` call from the picker result, or null if nothing was picked. */
    fun resolveSearchResult(call: PluginCall, result: ActivityResult) {
        val picked = searchResults(result).firstOrNull()
        call.resolve(jsObject { put("player", picked?.toJsObject() ?: JSObject.NULL) })
    }

    @Suppress("DEPRECATION") // the typed getParcelableArrayListExtra needs API 33; minSdk is 24.
    private fun searchResults(result: ActivityResult): List<PlayerEntity> =
        result.data
            ?.getParcelableArrayListExtra<PlayerEntity>(PlayersClient.EXTRA_PLAYER_SEARCH_RESULTS)
            .orEmpty()

    /**
     * `allowResolution` is consumed by the first attempt, so a granted consent retries
     * the load exactly once instead of looping if access is still withheld.
     */
    private fun loadFriendsPage(
        call: PluginCall,
        pageSize: Int,
        forceReload: Boolean,
        allowResolution: Boolean,
    ) {
        client.loadFriends(pageSize, forceReload)
            .addOnSuccessListener { annotated ->
                val friends = annotated.get()?.use { it.toJsArray(Player::toJsObject) } ?: JSArray()
                call.resolve(
                    jsObject {
                        put("friends", friends)
                        put("stale", annotated.isStale)
                        put("resolutionRequired", false)
                    },
                )
            }
            .addOnFailureListener { e ->
                if (e !is FriendsResolutionRequiredException) {
                    call.rejectFromException(e, "loadFriends failed")
                    return@addOnFailureListener
                }
                val launched = allowResolution &&
                    plugin.launchIntentSender(e.resolution) { granted ->
                        if (granted) {
                            loadFriendsPage(call, pageSize, forceReload, allowResolution = false)
                        } else {
                            resolveResolutionRequired(call)
                        }
                    }
                if (!launched) resolveResolutionRequired(call)
            }
    }

    private fun resolveResolutionRequired(call: PluginCall) {
        call.resolve(
            jsObject {
                put("friends", JSArray())
                put("stale", false)
                put("resolutionRequired", true)
            },
        )
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 25
    }
}
