package com.idleflowgames.playgames

import com.getcapacitor.PluginCall
import com.google.android.gms.games.Player
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.achievement.Achievement
import com.google.android.gms.games.gamessignin.AuthScope
import com.google.android.gms.games.leaderboard.Leaderboard
import com.google.android.gms.games.leaderboard.LeaderboardVariant

/**
 * Two-way lookup between a JS string union and the SDK value behind it. Every
 * enum-ish value the bridge carries is declared here so neither direction can
 * drift, and an unrecognised name is rejected rather than silently defaulted.
 */
internal class EnumCodec<T : Any>(vararg entries: Pair<String, T>) {
    private val byName: Map<String, T> = entries.toMap()
    private val byValue: Map<T, String> = entries.associate { (name, value) -> value to name }

    val names: String = byName.keys.joinToString(", ")

    /**
     * Null for a code this map does not cover, which is reachable rather than
     * theoretical: the collection constants are non-contiguous, `COLLECTION_PUBLIC`
     * being 0 and `COLLECTION_FRIENDS` 3, so a value outside the mapped set can arrive.
     * A serialiser whose *required* field comes back null must return null for the whole
     * object and let it drop out of its array, rather than hand JavaScript an object that
     * violates its own declared type: a missing entry is describable, a malformed one is not.
     */
    fun nameOf(value: T): String? = byValue[value]

    fun codeOf(name: String): T? = byName[name]
}

/** Read a string-union option; rejects and returns null when the name is unknown. */
internal fun <T : Any> PluginCall.enumOption(key: String, codec: EnumCodec<T>, default: T): T? {
    val name = getString(key) ?: return default
    val value = codec.codeOf(name)
    if (value == null) reject("unknown $key '$name'; expected one of ${codec.names}")
    return value
}

internal val TIME_SPANS = EnumCodec(
    "daily" to LeaderboardVariant.TIME_SPAN_DAILY,
    "weekly" to LeaderboardVariant.TIME_SPAN_WEEKLY,
    "allTime" to LeaderboardVariant.TIME_SPAN_ALL_TIME,
)

internal val COLLECTIONS = EnumCodec(
    "public" to LeaderboardVariant.COLLECTION_PUBLIC,
    "friends" to LeaderboardVariant.COLLECTION_FRIENDS,
)

internal val SCORE_ORDERS = EnumCodec(
    "smallerIsBetter" to Leaderboard.SCORE_ORDER_SMALLER_IS_BETTER,
    "largerIsBetter" to Leaderboard.SCORE_ORDER_LARGER_IS_BETTER,
)

internal val ACHIEVEMENT_STATES = EnumCodec(
    "unlocked" to Achievement.STATE_UNLOCKED,
    "revealed" to Achievement.STATE_REVEALED,
    "hidden" to Achievement.STATE_HIDDEN,
)

internal val ACHIEVEMENT_TYPES = EnumCodec(
    "standard" to Achievement.TYPE_STANDARD,
    "incremental" to Achievement.TYPE_INCREMENTAL,
)

internal val FRIEND_STATUSES = EnumCodec(
    "unknown" to Player.PlayerFriendStatus.UNKNOWN,
    "noRelationship" to Player.PlayerFriendStatus.NO_RELATIONSHIP,
    "friend" to Player.PlayerFriendStatus.FRIEND,
)

internal val FRIENDS_LIST_VISIBILITIES = EnumCodec(
    "unknown" to Player.FriendsListVisibilityStatus.UNKNOWN,
    "visible" to Player.FriendsListVisibilityStatus.VISIBLE,
    "requestRequired" to Player.FriendsListVisibilityStatus.REQUEST_REQUIRED,
    "featureUnavailable" to Player.FriendsListVisibilityStatus.FEATURE_UNAVAILABLE,
)

internal val SNAPSHOT_CONFLICT_POLICIES = EnumCodec(
    "mostRecentlyModified" to SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED,
    "longestPlaytime" to SnapshotsClient.RESOLUTION_POLICY_LONGEST_PLAYTIME,
    "lastKnownGood" to SnapshotsClient.RESOLUTION_POLICY_LAST_KNOWN_GOOD,
    "highestProgress" to SnapshotsClient.RESOLUTION_POLICY_HIGHEST_PROGRESS,
)

internal val AUTH_SCOPES = EnumCodec(
    "email" to AuthScope.EMAIL,
    "profile" to AuthScope.PROFILE,
    "openId" to AuthScope.OPEN_ID,
)
