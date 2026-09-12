import type { PluginListenerHandle } from "@capacitor/core";

/** A player's public profile. */
export interface PlayerInfo {
  /** Stable, platform-assigned Play Games player id. */
  playerId: string;
  /** Display name as shown in Google Play Games. */
  displayName: string;
  /** Small avatar URL, absent when the player has no icon image. */
  avatarUrl?: string;
  /** Full-resolution avatar URL, absent when the player has no hi-res image. */
  hiResImageUrl?: string;
  /** Landscape profile banner URL, absent when the player has none. */
  bannerImageLandscapeUrl?: string;
  /** Portrait profile banner URL, absent when the player has none. */
  bannerImagePortraitUrl?: string;
  /** Play Games title such as "Expert", absent when unset. */
  title?: string;
  /** Epoch ms this profile was fetched, absent when the SDK doesn't report one. */
  retrievedAt?: number;
  /** Epoch ms this player was last played with, absent when unknown. */
  lastPlayedWithAt?: number;
  /** Player level and XP progress, absent when the game has no level configuration. */
  level?: PlayerLevelInfo;
  /** Relationship to the signed-in player, absent when unknown. */
  friendStatus?: PlayerFriendStatus;
  /** Present only on the signed-in player's own profile. */
  friendsListVisibility?: FriendsListVisibility;
}

/** A player's level and XP progress within the game's configured level ladder. */
export interface PlayerLevelInfo {
  /** Absent when the SDK reports its CURRENT_XP_UNKNOWN sentinel. */
  currentXpTotal?: number;
  /** Epoch ms of the player's most recent level-up, absent when they haven't leveled up. */
  lastLevelUpAt?: number;
  /** Whether the player has reached the highest configured level. */
  isMaxLevel: boolean;
  /** The level the player is currently in. */
  currentLevel: PlayerLevel;
  /** The next level up; equal to `currentLevel` when `isMaxLevel` is true. */
  nextLevel: PlayerLevel;
}

/** One level's XP boundaries in the game's configured level ladder. */
export interface PlayerLevel {
  levelNumber: number;
  minXp: number;
  maxXp: number;
}

/** A player's relationship to the signed-in player. */
export type PlayerFriendStatus = "unknown" | "noRelationship" | "friend";

/** Whether the signed-in player's friends list is visible to this game. */
export type FriendsListVisibility = "unknown" | "visible" | "requestRequired" | "featureUnavailable";

/** Result of a sign-in attempt, or the payload of a sign-in state change. */
export interface SignInResult {
  /** Whether the player is currently authenticated. */
  signedIn: boolean;
  /** The player profile, present only when `signedIn` is true. */
  player?: PlayerInfo;
}

/** Result of {@link PlayGamesPlugin.loadFriends}. */
export interface FriendsResult {
  /** The requested page of friends, empty when `resolutionRequired` is true. */
  friends: PlayerInfo[];
  /** Whether this result may already be out of date on the server. */
  stale: boolean;
  /**
   * True when the player has not granted friends-list access. `friends` is
   * empty; call `loadFriends` again with `resolve: true` to show the consent UI.
   */
  resolutionRequired: boolean;
}

/** Unlock state of an achievement. */
export type AchievementState = "unlocked" | "revealed" | "hidden";

/** A single-unlock achievement's Play Console metadata plus the signed-in player's progress on it. */
export interface StandardAchievement {
  /** Play Console achievement id. */
  id: string;
  type: "standard";
  name: string;
  description: string;
  state: AchievementState;
  /** XP granted by unlocking this achievement. */
  xpValue: number;
  /** Epoch ms this achievement's state last changed. */
  lastUpdatedAt: number;
  /** Icon shown once unlocked, absent when the SDK reports none. */
  unlockedImageUrl?: string;
  /** Icon shown while hidden or revealed, absent when the SDK reports none. */
  revealedImageUrl?: string;
}

/** A stepped-counter achievement's Play Console metadata plus the signed-in player's step progress on it. */
export interface IncrementalAchievement {
  /** Play Console achievement id. */
  id: string;
  type: "incremental";
  name: string;
  description: string;
  state: AchievementState;
  /** XP granted by fully unlocking this achievement. */
  xpValue: number;
  /** Epoch ms this achievement's state last changed. */
  lastUpdatedAt: number;
  /** Icon shown once unlocked, absent when the SDK reports none. */
  unlockedImageUrl?: string;
  /** Icon shown while hidden or revealed, absent when the SDK reports none. */
  revealedImageUrl?: string;
  currentSteps: number;
  totalSteps: number;
  formattedCurrentSteps: string;
  formattedTotalSteps: string;
}

/**
 * An achievement's Play Console metadata plus the signed-in player's progress
 * on it. Narrow on `type` to reach the incremental-only step fields — they
 * don't exist on a standard achievement rather than being merely absent.
 */
export type Achievement = StandardAchievement | IncrementalAchievement;

/** Whether an achievement is a single unlock or a stepped counter. */
export type AchievementType = Achievement["type"];

/** Which score window a leaderboard read or write applies to. */
export type LeaderboardTimeSpan = "daily" | "weekly" | "allTime";

/** Which scope of players a leaderboard read applies to. */
export type LeaderboardCollection = "public" | "friends";

/** Whether a lower or higher raw score ranks better on a leaderboard. */
export type LeaderboardScoreOrder = "smallerIsBetter" | "largerIsBetter";

/** A leaderboard's Play Console metadata. */
export interface Leaderboard {
  leaderboardId: string;
  displayName: string;
  /** Absent when the SDK reports none. */
  iconImageUrl?: string;
  /** Play Console policy, not an SDK guarantee — see "Leaderboards" in the README's Limits section. */
  scoreOrder: LeaderboardScoreOrder;
  /** One entry per time-span/collection combination the SDK reports. */
  variants: LeaderboardVariant[];
}

/** One time-span/collection slice of a leaderboard, with the signed-in player's standing in it. */
export interface LeaderboardVariant {
  timeSpan: LeaderboardTimeSpan;
  collection: LeaderboardCollection;
  /** Whether the signed-in player has a score in this variant. */
  hasPlayerInfo: boolean;
  /** Absent when the SDK reports its PLAYER_SCORE_UNKNOWN sentinel. */
  playerScore?: number;
  /** Absent under the same condition as `playerScore`. */
  displayPlayerScore?: string;
  /** Absent when the SDK reports its PLAYER_RANK_UNKNOWN sentinel. */
  playerRank?: number;
  /** Absent under the same condition as `playerRank`. */
  displayPlayerRank?: string;
  /** Absent when the signed-in player has no score tag in this variant. */
  playerScoreTag?: string;
  /** Absent when the SDK reports its NUM_SCORES_UNKNOWN sentinel. */
  numScores?: number;
}

/** One entry in a leaderboard's score list. */
export interface LeaderboardScore {
  /** Absent when the SDK reports its LEADERBOARD_RANK_UNKNOWN sentinel. */
  rank?: number;
  displayRank: string;
  rawScore: number;
  displayScore: string;
  /** Epoch ms this score was recorded. */
  achievedAt: number;
  scoreTag: string;
  /** The player who holds this score, absent when the SDK doesn't attach one. */
  scoreHolder?: PlayerInfo;
  scoreHolderDisplayName: string;
  /** Absent when the score holder has no icon image. */
  scoreHolderIconImageUrl?: string;
  /** Absent when the score holder has no hi-res image. */
  scoreHolderHiResImageUrl?: string;
}

/** Options shared by {@link PlayGamesPlugin.loadTopScores} and {@link PlayGamesPlugin.loadPlayerCenteredScores}. */
export interface LoadScoresOptions {
  leaderboardId: string;
  /** Defaults to "allTime". */
  timeSpan?: LeaderboardTimeSpan;
  /** Defaults to "public". */
  collection?: LeaderboardCollection;
  /**
   * Defaults to 25. Values outside 1-25 are not validated by this plugin,
   * and whether the SDK clamps or rejects an out-of-range value was not
   * confirmed.
   */
  maxResults?: number;
  /** Defaults to `false`. */
  forceReload?: boolean;
}

/** Result of {@link PlayGamesPlugin.loadTopScores} and {@link PlayGamesPlugin.loadPlayerCenteredScores}. */
export interface LoadScoresResult {
  /** The leaderboard's own metadata, absent when the SDK doesn't attach it. */
  leaderboard: Leaderboard | null;
  scores: LeaderboardScore[];
  stale: boolean;
}

/** Result of a leaderboard score submission. */
export interface ScoreSubmissionResult {
  leaderboardId: string;
  playerId: string;
  /**
   * One entry per time span the submission affected. The SDK does not
   * guarantee all three (daily/weekly/all-time) are present, and does not
   * report why a given one might be missing.
   */
  results: {
    timeSpan: LeaderboardTimeSpan;
    rawScore: number;
    formattedScore: string;
    scoreTag: string;
    /** Whether this score is now the player's best for that time span. */
    newBest: boolean;
  }[];
}

/**
 * Automatic saved-game conflict resolution strategy, applied when two devices
 * wrote the same snapshot before either saw the other's write. Manual
 * conflict resolution (inspecting both copies in JS) isn't exposed — see the
 * README for why — so one of these always applies.
 */
export type SnapshotConflictPolicy = "mostRecentlyModified" | "longestPlaytime" | "lastKnownGood" | "highestProgress";

/** Snapshot metadata without the payload, as returned by `listSnapshots` and `showSnapshots`. */
export interface SnapshotMeta {
  /** Stable unique name the snapshot was saved under. */
  name: string;
  /** Platform-assigned snapshot id, distinct from `name`. */
  snapshotId: string;
  description: string;
  /** Last-modified time, in epoch milliseconds. */
  modifiedAt: number;
  /** Absent when the SDK reports its PLAYED_TIME_UNKNOWN sentinel. */
  playedTimeMillis?: number;
  /** Absent when the SDK reports its PROGRESS_VALUE_UNKNOWN sentinel. */
  progressValue?: number;
  /** Name of the device the snapshot was last written from, absent when unknown. */
  deviceName?: string;
  /** Absent when the snapshot has no cover image. */
  coverImageUrl?: string;
  /** Absent under the same condition as `coverImageUrl`. */
  coverImageAspectRatio?: number;
  /** Whether a change to this snapshot is pending sync to Google's servers. */
  hasChangePending: boolean;
}

/** A saved-game snapshot together with its serialized payload. */
export interface Snapshot extends SnapshotMeta {
  /** The serialized save payload as a UTF-8 string (encode binary yourself). */
  data: string;
}

/**
 * One property on a game-stats event. The four shapes map one-to-one onto the
 * four `PlayerGameEvent.Builder.addProperty` overloads; the tag is required
 * because a Play Console stat aggregates by the property's declared type and
 * guessing int-vs-double from a JS number would silently pick the wrong one.
 *
 * `int` values are passed through as a 64-bit native long; values outside
 * `Number.MAX_SAFE_INTEGER` will lose precision crossing the JS/native bridge.
 */
export type GameEventValue =
  | { int: number; double?: never; string?: never; bool?: never }
  | { double: number; int?: never; string?: never; bool?: never }
  | { string: string; int?: never; double?: never; bool?: never }
  | { bool: boolean; int?: never; double?: never; string?: never };

/** Named properties attached to a game-stats event, keyed by property key. */
export type GameEventProperties = Record<string, GameEventValue>;

/** One game-stats event to record. */
export interface GameEvent {
  name: string;
  properties?: GameEventProperties;
}

/** A Play Console *event* resource — the legacy events feature, distinct from game stats. */
export interface GameEventDefinition {
  eventId: string;
  name: string;
  description: string;
  /** Absent when the SDK reports none. */
  iconImageUrl?: string;
  /** Cumulative count the SDK has recorded for this event. */
  value: number;
  formattedValue: string;
  visible: boolean;
}

/**
 * Player-level engagement and spend predictions from Play Games Services.
 * Every field is absent when the SDK reports its UNSET_VALUE sentinel.
 */
export interface PlayerStats {
  averageSessionLength?: number;
  churnProbability?: number;
  daysSinceLastPlayed?: number;
  numberOfPurchases?: number;
  numberOfSessions?: number;
  sessionPercentile?: number;
  spendPercentile?: number;
  spendProbability?: number;
  highSpenderProbability?: number;
  totalSpendNext28Days?: number;
}

/** An OAuth 2.0 scope that can be requested alongside a server auth code. */
export type AuthScopeName = "email" | "profile" | "openId";

/** Options accepted by read methods whose only option is bypassing the SDK's cache. */
export interface ForceReloadOptions {
  /** Defaults to `false`. */
  forceReload?: boolean;
}

/** Payload of the `signInStateChanged` event. */
export type SignInStateChangedEvent = SignInResult;

export interface PlayGamesPlugin {
  /**
   * No-op. `PlayGamesSdk.initialize` runs automatically when the plugin
   * loads, driven by the Capacitor bridge — this call exists only to keep
   * the API symmetric with the web fallback.
   *
   * @since 0.1.0
   */
  initialize(): Promise<void>;
  /**
   * Sign in to Google Play Games.
   *
   * `silent` (default `true`) attempts auto sign-in with no UI; on most devices
   * this succeeds if the player has previously authenticated this game. Pass
   * `silent: false` to force the full interactive flow, and only in response to
   * an explicit user gesture.
   *
   * @since 0.1.0
   */
  signIn(opts?: {
    silent?: boolean;
  }): Promise<SignInResult>;
  /**
   * Whether a player is currently signed in.
   * @since 0.1.0
   */
  isSignedIn(): Promise<{
    signedIn: boolean;
  }>;
  /**
   * Get the signed-in player's profile.
   *
   * Rejects when no player is signed in. On web (the no-op fallback) it
   * resolves an empty profile (`playerId: ""`).
   * @since 0.1.0
   */
  getPlayer(): Promise<PlayerInfo>;
  /**
   * Get the signed-in player's id without loading the rest of their profile.
   *
   * The web fallback resolves an empty `playerId`.
   * @since 0.5.0
   */
  getPlayerId(): Promise<{
    playerId: string;
  }>;
  /**
   * Load another player's profile by their Play Games player id. Resolves
   * `{ player: null }` when the lookup can't find that player.
   *
   * `forceReload` (default `false`) bypasses any cache the SDK is holding.
   * The web fallback resolves `{ player: null, stale: false }`.
   * @since 0.5.0
   */
  loadPlayer(opts: {
    playerId: string;
    forceReload?: boolean;
  }): Promise<{
    player: PlayerInfo | null;
    stale: boolean;
  }>;
  /**
   * Load a page of the signed-in player's Play Games friends.
   *
   * `pageSize` (default 25) bounds the page. `forceReload` (default `false`)
   * bypasses any cache the SDK is holding. `resolve` (default `false`)
   * launches the consent UI when friends-list access hasn't been granted yet;
   * without it, that case resolves `{ friends: [], resolutionRequired: true }`
   * rather than rejecting — reading the friends list never puts an account
   * dialog in front of the player unless you opt in with `resolve: true`.
   * @since 0.5.0
   */
  loadFriends(opts?: {
    pageSize?: number;
    forceReload?: boolean;
    resolve?: boolean;
  }): Promise<FriendsResult>;
  /**
   * Load players the signed-in player has recently played a match against.
   *
   * `pageSize` (default 25) bounds the page. `forceReload` (default `false`)
   * bypasses any cache the SDK is holding.
   * @since 0.5.0
   */
  loadRecentlyPlayedWithPlayers(opts?: {
    pageSize?: number;
    forceReload?: boolean;
  }): Promise<{
    players: PlayerInfo[];
    stale: boolean;
  }>;
  /**
   * Show the native player search UI. Resolves `{ player: null }` when the
   * player cancels without picking anyone.
   * @since 0.5.0
   */
  showPlayerSearch(): Promise<{
    player: PlayerInfo | null;
  }>;
  /**
   * Show the native UI comparing the signed-in player's profile against another
   * player's.
   *
   * `otherPlayerInGameName`/`currentPlayerInGameName` are optional in-game
   * display name hints shown alongside each player's Play Games name; supply
   * both or neither.
   * @since 0.5.0
   */
  showComparePlayer(opts: {
    playerId: string;
    otherPlayerInGameName?: string;
    currentPlayerInGameName?: string;
  }): Promise<void>;
  /**
   * Request a one-time OAuth 2.0 server auth code for the signed-in Play Games
   * player, for a backend to exchange for the AUTHORITATIVE player id (Google Play
   * Games Services v2 `GamesSignInClient.requestServerSideAccess`).
   *
   * `serverClientId` is the OAuth 2.0 **web** client id backing the game; the code
   * is redeemed against it server-side. `forceRefresh` (default `false`) requests a
   * fresh code even if one was recently granted. Passing `scopes` additionally
   * requests consent for those OAuth scopes; the result's `grantedScopes` is
   * present only when `scopes` was passed, and lists what the player actually
   * granted (which can be a subset of what was requested).
   *
   * The web fallback resolves an empty `authCode`.
   * @since 0.2.0
   */
  requestServerSideAccess(opts: {
    serverClientId: string;
    forceRefresh?: boolean;
    scopes?: AuthScopeName[];
  }): Promise<{
    authCode: string;
    grantedScopes?: AuthScopeName[];
  }>;
  /**
   * Unlock an achievement by its Play Console achievement id.
   *
   * Resolves only once the server has recorded the unlock (the SDK's
   * `unlockImmediate`).
   * @since 0.1.0
   */
  unlockAchievement(opts: {
    id: string;
  }): Promise<void>;
  /**
   * Reveal a hidden achievement without unlocking it.
   *
   * Resolves only once the server has recorded the reveal (the SDK's
   * `revealImmediate`).
   * @since 0.5.0
   */
  revealAchievement(opts: {
    id: string;
  }): Promise<void>;
  /**
   * Increment a partial (incremental) achievement.
   *
   * `steps` is a discrete step count toward the achievement's Play Console
   * step total, and must be greater than 0 — the call rejects otherwise.
   *
   * Resolves only once the server has recorded the increment (the SDK's
   * `incrementImmediate`). `unlocked` is `true` when this increment caused
   * the achievement to become fully unlocked.
   * @since 0.1.0
   */
  incrementAchievement(opts: {
    id: string;
    steps: number;
  }): Promise<{
    unlocked: boolean;
  }>;
  /**
   * Set the absolute step count of an incremental achievement, rather than
   * incrementing it by a delta. `steps` must be greater than 0 — the call
   * rejects otherwise.
   *
   * Resolves only once the server has recorded the new step count (the
   * SDK's `setStepsImmediate`). `unlocked` is `true` when this call caused
   * the achievement to become fully unlocked.
   * @since 0.5.0
   */
  setAchievementSteps(opts: {
    id: string;
    steps: number;
  }): Promise<{
    unlocked: boolean;
  }>;
  /**
   * Load the signed-in player's achievements, with their current unlock state
   * and (for incremental achievements) step progress.
   *
   * `forceReload` (default `false`) bypasses any cache the SDK is holding.
   * @since 0.5.0
   */
  loadAchievements(opts?: ForceReloadOptions): Promise<{
    achievements: Achievement[];
    stale: boolean;
  }>;
  /**
   * Show the native Google Play Games achievements UI.
   * @since 0.1.0
   */
  showAchievements(): Promise<void>;
  /**
   * Submit a score to a leaderboard by its Play Console leaderboard id.
   *
   * `scoreTag` is an optional opaque string (e.g. a replay id) stored
   * alongside the score and returned with it later.
   *
   * Resolves only once the server has recorded the score (the SDK's
   * `submitScoreImmediate`); see `ScoreSubmissionResult` for the shape of
   * the result.
   * @since 0.1.0
   */
  submitScore(opts: {
    leaderboardId: string;
    score: number;
    scoreTag?: string;
  }): Promise<ScoreSubmissionResult>;
  /**
   * Show the native UI for a single leaderboard.
   *
   * `timeSpan` and `collection` preselect which slice of the leaderboard opens;
   * both default to the SDK's own default view when omitted.
   * @since 0.1.0
   */
  showLeaderboard(opts: {
    leaderboardId: string;
    timeSpan?: LeaderboardTimeSpan;
    collection?: LeaderboardCollection;
  }): Promise<void>;
  /**
   * Show the native all-leaderboards UI.
   * @since 0.1.0
   */
  showAllLeaderboards(): Promise<void>;
  /**
   * Load metadata for all of the game's leaderboards.
   *
   * `forceReload` (default `false`) bypasses any cache the SDK is holding.
   * @since 0.5.0
   */
  loadLeaderboards(opts?: ForceReloadOptions): Promise<{
    leaderboards: Leaderboard[];
    stale: boolean;
  }>;
  /**
   * Load metadata for a single leaderboard. Resolves `{ leaderboard: null }`
   * when no leaderboard exists for that id.
   *
   * `forceReload` (default `false`) bypasses any cache the SDK is holding.
   * @since 0.5.0
   */
  loadLeaderboard(opts: {
    leaderboardId: string;
    forceReload?: boolean;
  }): Promise<{
    leaderboard: Leaderboard | null;
    stale: boolean;
  }>;
  /**
   * Load the top scores on a leaderboard.
   * @since 0.5.0
   */
  loadTopScores(opts: LoadScoresOptions): Promise<LoadScoresResult>;
  /**
   * Load the scores immediately above and below the signed-in player's own
   * score on a leaderboard.
   * @since 0.5.0
   */
  loadPlayerCenteredScores(opts: LoadScoresOptions): Promise<LoadScoresResult>;
  /**
   * Load the signed-in player's own score on a leaderboard. Resolves
   * `{ score: null }` when the player has no score in that time span/collection.
   * @since 0.5.0
   */
  loadCurrentPlayerScore(opts: {
    leaderboardId: string;
    timeSpan?: LeaderboardTimeSpan;
    collection?: LeaderboardCollection;
  }): Promise<{
    score: LeaderboardScore | null;
    stale: boolean;
  }>;
  /**
   * Load a saved-game snapshot by its stable name. Resolves `{ snapshot: null }`
   * when no snapshot exists for that name.
   *
   * `conflictPolicy` (default "mostRecentlyModified") picks the automatic
   * strategy used if this device and another wrote the snapshot before either
   * saw the other's write.
   * @since 0.1.0
   */
  loadSnapshot(opts: {
    name: string;
    conflictPolicy?: SnapshotConflictPolicy;
  }): Promise<{
    snapshot: Snapshot | null;
  }>;
  /**
   * Create or overwrite a saved-game snapshot.
   *
   * `playedTimeMillis` and `progressValue` are opaque numbers you define and
   * that surface in the native snapshot-picker UI; `coverImage` is a base64
   * PNG/JPEG with no `data:` URI prefix. `conflictPolicy` (default
   * "mostRecentlyModified") picks the automatic strategy used if this device
   * and another wrote the snapshot before either saw the other's write —
   * manual conflict resolution isn't exposed, see the README.
   * @since 0.1.0
   */
  saveSnapshot(opts: {
    name: string;
    data: string;
    description?: string;
    playedTimeMillis?: number;
    progressValue?: number;
    coverImage?: string;
    conflictPolicy?: SnapshotConflictPolicy;
  }): Promise<void>;
  /**
   * List metadata for all of the player's snapshots.
   *
   * `forceReload` (default `false`) bypasses any cache the SDK is holding.
   * @since 0.1.0
   */
  listSnapshots(opts?: ForceReloadOptions): Promise<{
    snapshots: SnapshotMeta[];
  }>;
  /**
   * Delete a saved-game snapshot by its stable name.
   * @since 0.1.0
   */
  deleteSnapshot(opts: {
    name: string;
  }): Promise<void>;
  /**
   * Show the native saved-games UI for picking, and optionally creating or
   * deleting, a snapshot. Resolves `{ snapshot: null }` when the player
   * cancels without picking one.
   *
   * `title` (default `"Saved games"`) labels the picker; `allowAdd` and
   * `allowDelete` (both default `true`) show or hide those actions;
   * `maxSnapshots` (default: no limit) caps how many existing snapshots are
   * listed.
   * @since 0.5.0
   */
  showSnapshots(opts?: {
    title?: string;
    allowAdd?: boolean;
    allowDelete?: boolean;
    maxSnapshots?: number;
  }): Promise<{
    snapshot: SnapshotMeta | null;
    isNew: boolean;
  }>;
  /**
   * Get the platform's per-snapshot size limits, read from the device at
   * call time rather than hardcoded, so they can't go stale if Google
   * changes them. See "Saved games" in the README's Limits section for
   * Google's documented figures and an important caveat about the unit
   * these two numbers are in.
   *
   * The web fallback resolves both as `0`. Check `isSignedIn()` before
   * treating either as a real limit, because comparing a payload length
   * against a zero reports every save as oversized.
   * @since 0.5.0
   */
  getSnapshotLimits(): Promise<{
    maxDataSize: number;
    maxCoverImageSize: number;
  }>;
  /**
   * Record one game-stats event, rejecting if it's over the limits in
   * "Game stats" in the README's Limits section (event name length,
   * property count, property key/value length) — all enforced natively.
   *
   * This call is fire-and-forget: the native SDK method returns no result, so
   * a resolved promise means the event was queued on the device, not that
   * Google has received it. Call `requestGameEventsUpload` to flush the queue;
   * anything not yet uploaded is lost if the app crashes first.
   * @since 0.5.0
   */
  recordGameEvent(opts: {
    name: string;
    properties?: GameEventProperties;
  }): Promise<void>;
  /**
   * Record a batch of game-stats events in one native call, subject to the
   * same per-event limits as `recordGameEvent` plus a limit on the batch
   * itself — see "Game stats" in the README's Limits section.
   *
   * Fire-and-forget for the same reason as `recordGameEvent`: a resolved
   * promise means the batch was queued, not delivered.
   * @since 0.5.0
   */
  recordGameEvents(opts: {
    events: GameEvent[];
  }): Promise<void>;
  /**
   * Record a game-stats progress-update event: shorthand for `recordGameEvent`
   * with the fixed event name `progressUpdate` and an int property
   * `currentProgress` set to this value, subject to the same limits (see
   * "Game stats" in the README's Limits section).
   *
   * Fire-and-forget for the same reason as `recordGameEvent`.
   * @since 0.5.0
   */
  recordProgressUpdate(opts: {
    currentProgress: number;
    properties?: GameEventProperties;
  }): Promise<void>;
  /**
   * Flush any game-stats events queued by `recordGameEvent`/`recordGameEvents`/
   * `recordProgressUpdate` to Google's servers. This is the only confirmation
   * point in the game-stats API; nothing else in it reports delivery.
   * @since 0.5.0
   */
  requestGameEventsUpload(): Promise<void>;
  /**
   * Increment a legacy Play Console *event* (not a game-stats event) by
   * `amount`. Like the native SDK method, this is fire-and-forget: a resolved
   * promise means the request was made, not that the server has recorded it.
   * @since 0.5.0
   */
  incrementEvent(opts: {
    eventId: string;
    amount: number;
  }): Promise<void>;
  /**
   * Load all of the game's legacy Play Console events and the signed-in
   * player's cumulative counts on them.
   *
   * `forceReload` (default `false`) bypasses any cache the SDK is holding.
   * @since 0.5.0
   */
  loadEvents(opts?: ForceReloadOptions): Promise<{
    events: GameEventDefinition[];
    stale: boolean;
  }>;
  /**
   * Load specific legacy Play Console events by id. Rejects if `eventIds` is
   * empty.
   *
   * `forceReload` (default `false`) bypasses any cache the SDK is holding.
   * @since 0.5.0
   */
  loadEventsByIds(opts: {
    eventIds: string[];
    forceReload?: boolean;
  }): Promise<{
    events: GameEventDefinition[];
    stale: boolean;
  }>;
  /**
   * Request a Play Games Recall session id: an opaque identifier from
   * `RecallClient`, obtained without requiring the player to sign in to Play
   * Games first.
   *
   * The web fallback resolves an empty `sessionId`; don't forward it to a
   * backend as if it were a real session identifier.
   * @since 0.5.0
   */
  requestRecallAccess(): Promise<{
    sessionId: string;
  }>;
  /**
   * Load Play Games' engagement and spend predictions for the signed-in
   * player.
   *
   * `forceReload` (default `false`) bypasses any cache the SDK is holding.
   * @since 0.5.0
   */
  loadPlayerStats(opts?: ForceReloadOptions): Promise<{
    stats: PlayerStats;
    stale: boolean;
  }>;
  /**
   * Listen for sign-in state changes: an interactive sign-in completing, or the
   * player signing out of Google Play Games system-wide.
   * @since 0.1.0
   */
  addListener(event: "signInStateChanged", listener: (e: SignInStateChangedEvent) => void): Promise<PluginListenerHandle>;
  /**
   * Remove all listeners registered through this plugin.
   * @since 0.1.0
   */
  removeAllListeners(): Promise<void>;
}
