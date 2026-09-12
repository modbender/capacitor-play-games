package com.idleflowgames.playgames

import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.leaderboard.Leaderboard
import com.google.android.gms.games.leaderboard.LeaderboardScore
import com.google.android.gms.games.leaderboard.LeaderboardVariant
import com.google.android.gms.games.leaderboard.ScoreSubmissionData

internal class LeaderboardsModule(plugin: PlayGamesPlugin) : PgsModule(plugin) {
    private val client get() = PlayGames.getLeaderboardsClient(activity)

    fun submit(call: PluginCall) {
        val id = call.requireString("leaderboardId") ?: return
        val score = call.requireNumber("score")?.toLong() ?: return
        val scoreTag = call.getString("scoreTag")
        val task = if (scoreTag == null) {
            client.submitScoreImmediate(id, score)
        } else {
            client.submitScoreImmediate(id, score, scoreTag)
        }
        task.bind(call, "submitScore failed") { it.toJsObject() }
    }

    fun show(call: PluginCall) {
        val id = call.requireString("leaderboardId") ?: return
        // The one- and two-argument intents let the PGS UI choose the time span and
        // collection it shows, which is not the same thing as asking for all-time and
        // public; widen to a longer overload only when the caller named one.
        val wantsTimeSpan = call.getString("timeSpan") != null
        val wantsCollection = call.getString("collection") != null
        val timeSpan = call.timeSpanOption() ?: return
        val collection = call.collectionOption() ?: return
        val intent = when {
            wantsCollection -> client.getLeaderboardIntent(id, timeSpan, collection)
            wantsTimeSpan -> client.getLeaderboardIntent(id, timeSpan)
            else -> client.getLeaderboardIntent(id)
        }
        plugin.launchUiIntent(intent, call, "onLeaderboardUiResult")
    }

    fun showAll(call: PluginCall) {
        plugin.launchUiIntent(client.allLeaderboardsIntent, call, "onLeaderboardUiResult")
    }

    fun loadAll(call: PluginCall) {
        val forceReload = call.forceReload()
        client.loadLeaderboardMetadata(forceReload).bindAnnotated(call, "loadLeaderboards failed") { buffer ->
            jsObject {
                put("leaderboards", buffer?.use { it.toJsArray(Leaderboard::toJsObject) } ?: JSArray())
            }
        }
    }

    fun loadOne(call: PluginCall) {
        val id = call.requireString("leaderboardId") ?: return
        val forceReload = call.forceReload()
        client.loadLeaderboardMetadata(id, forceReload).bindAnnotated(call, "loadLeaderboard failed") { board ->
            jsObject { put("leaderboard", board?.toJsObject() ?: JSObject.NULL) }
        }
    }

    fun loadTopScores(call: PluginCall) = loadScores(call, centered = false)

    fun loadPlayerCenteredScores(call: PluginCall) = loadScores(call, centered = true)

    fun loadCurrentPlayerScore(call: PluginCall) {
        val id = call.requireString("leaderboardId") ?: return
        val timeSpan = call.timeSpanOption() ?: return
        val collection = call.collectionOption() ?: return
        client.loadCurrentPlayerLeaderboardScore(id, timeSpan, collection)
            .bindAnnotated(call, "loadCurrentPlayerScore failed") { score ->
                jsObject { put("score", score?.toJsObject() ?: JSObject.NULL) }
            }
    }

    private fun loadScores(call: PluginCall, centered: Boolean) {
        val id = call.requireString("leaderboardId") ?: return
        val timeSpan = call.timeSpanOption() ?: return
        val collection = call.collectionOption() ?: return
        val maxResults = call.intOption("maxResults", DEFAULT_MAX_RESULTS) ?: return
        val forceReload = call.forceReload()
        val task = if (centered) {
            client.loadPlayerCenteredScores(id, timeSpan, collection, maxResults, forceReload)
        } else {
            client.loadTopScores(id, timeSpan, collection, maxResults, forceReload)
        }
        task.bindAnnotated(call, "loadScores failed") { held ->
            val payload = JSObject()
            val entries = JSArray()
            var leaderboard: JSObject? = null
            held?.use { scores ->
                leaderboard = scores.leaderboard?.toJsObject()
                // The holder and the score buffer it hands out are two separate
                // Releasables and the holder is not documented to cascade, so both are
                // released. A second release costs nothing: AbstractDataBuffer.release()
                // null-checks its holder and calls DataHolder.close(), which is
                // synchronized and returns immediately once its closed flag is set.
                scores.scores.use { buffer ->
                    for (score in buffer) entries.put(score.toJsObject())
                }
            }
            payload.put("leaderboard", leaderboard ?: JSObject.NULL)
            payload.put("scores", entries)
            payload
        }
    }

    private companion object {
        const val DEFAULT_MAX_RESULTS = 25
    }
}

private fun PluginCall.timeSpanOption(): Int? =
    enumOption("timeSpan", TIME_SPANS, LeaderboardVariant.TIME_SPAN_ALL_TIME)

private fun PluginCall.collectionOption(): Int? =
    enumOption("collection", COLLECTIONS, LeaderboardVariant.COLLECTION_PUBLIC)

internal fun Leaderboard.toJsObject(): JSObject? {
    val order = SCORE_ORDERS.nameOf(scoreOrder) ?: return null
    return jsObject {
        put("leaderboardId", leaderboardId)
        put("displayName", displayName.orEmpty())
        putIfPresent("iconImageUrl", iconImageUri)
        put("scoreOrder", order)
        put("variants", variants.orEmpty().toJsArray(LeaderboardVariant::toJsObject))
    }
}

internal fun LeaderboardVariant.toJsObject(): JSObject? {
    val span = TIME_SPANS.nameOf(timeSpan) ?: return null
    val scope = COLLECTIONS.nameOf(collection) ?: return null
    return jsObject {
        put("timeSpan", span)
        put("collection", scope)
        put("hasPlayerInfo", hasPlayerInfo())
        putUnlessSentinel("playerScore", rawPlayerScore, LeaderboardVariant.PLAYER_SCORE_UNKNOWN.toLong())
        putIfPresent("displayPlayerScore", displayPlayerScore)
        putUnlessSentinel("playerRank", playerRank, LeaderboardVariant.PLAYER_RANK_UNKNOWN.toLong())
        putIfPresent("displayPlayerRank", displayPlayerRank)
        putIfPresent("playerScoreTag", playerScoreTag)
        putUnlessSentinel("numScores", numScores, LeaderboardVariant.NUM_SCORES_UNKNOWN.toLong())
    }
}

internal fun LeaderboardScore.toJsObject(): JSObject = jsObject {
    putUnlessSentinel("rank", rank, LeaderboardScore.LEADERBOARD_RANK_UNKNOWN.toLong())
    put("displayRank", displayRank.orEmpty())
    put("rawScore", rawScore)
    put("displayScore", displayScore.orEmpty())
    put("achievedAt", timestampMillis)
    put("scoreTag", scoreTag.orEmpty())
    put("scoreHolderDisplayName", scoreHolderDisplayName.orEmpty())
    putIfPresent("scoreHolderIconImageUrl", scoreHolderIconImageUri)
    putIfPresent("scoreHolderHiResImageUrl", scoreHolderHiResImageUri)
    putIfPresent("scoreHolder", scoreHolder?.toJsObject())
}

internal fun ScoreSubmissionData.toJsObject(): JSObject = jsObject {
    put("leaderboardId", leaderboardId)
    put("playerId", playerId)
    val results = JSArray()
    for (timeSpan in SUBMISSION_TIME_SPANS) {
        // Declared @NonNull upstream; read through a nullable local so a broken
        // contract degrades to a missing span rather than an exception.
        val result: ScoreSubmissionData.Result? = getScoreResult(timeSpan)
        if (result == null) continue
        val span = TIME_SPANS.nameOf(timeSpan) ?: continue
        results.put(
            jsObject {
                put("timeSpan", span)
                put("rawScore", result.rawScore)
                put("formattedScore", result.formattedScore.orEmpty())
                put("scoreTag", result.scoreTag.orEmpty())
                put("newBest", result.newBest)
            },
        )
    }
    put("results", results)
}

private val SUBMISSION_TIME_SPANS = intArrayOf(
    LeaderboardVariant.TIME_SPAN_DAILY,
    LeaderboardVariant.TIME_SPAN_WEEKLY,
    LeaderboardVariant.TIME_SPAN_ALL_TIME,
)
