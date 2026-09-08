import type { PluginListenerHandle } from "@capacitor/core";

/** A signed-in player's public profile. */
export interface PlayerInfo {
  /** Stable, platform-assigned Play Games player id. */
  playerId: string;
  /** Display name as shown in Google Play Games. */
  displayName: string;
  /** URL of the player's avatar image, when the platform exposes one. */
  avatarUrl?: string;
}

/** Result of a sign-in attempt, or the payload of a sign-in state change. */
export interface SignInResult {
  /** Whether the player is currently authenticated. */
  signedIn: boolean;
  /** The player profile, present only when `signedIn` is true. */
  player?: PlayerInfo;
}

/** A saved-game snapshot together with its serialized payload. */
export interface Snapshot {
  /** Stable unique name the snapshot was saved under. */
  name: string;
  /** Human-readable description stored with the snapshot. */
  description: string;
  /** Last-modified time, in epoch milliseconds. */
  modifiedAt: number;
  /** The serialized save payload as a UTF-8 string (encode binary yourself). */
  data: string;
}

/** Snapshot metadata without the payload, as returned by `listSnapshots`. */
export interface SnapshotMeta {
  /** Stable unique name of the snapshot. */
  name: string;
  /** Human-readable description stored with the snapshot. */
  description: string;
  /** Last-modified time, in epoch milliseconds. */
  modifiedAt: number;
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
   * Request a one-time OAuth 2.0 server auth code for the signed-in Play Games
   * player, for a backend to exchange for the AUTHORITATIVE player id (Google Play
   * Games Services v2 `GamesSignInClient.requestServerSideAccess`).
   *
   * `serverClientId` is the OAuth 2.0 **web** client id backing the game; the code
   * is redeemed against it server-side. `forceRefresh` (default `false`) requests a
   * fresh code even if one was recently granted.
   *
   * The web fallback resolves an empty `authCode`.
   * @since 0.2.0
   */
  requestServerSideAccess(opts: {
    serverClientId: string;
    forceRefresh?: boolean;
  }): Promise<{
    authCode: string;
  }>;
  /**
   * Unlock an achievement by its Play Console achievement id.
   * @since 0.1.0
   */
  unlockAchievement(opts: {
    id: string;
  }): Promise<void>;
  /**
   * Increment a partial (incremental) achievement.
   *
   * `steps` is a discrete step count toward the achievement's Play Console
   * step total, and must be greater than 0 — the call rejects otherwise.
   * @since 0.1.0
   */
  incrementAchievement(opts: {
    id: string;
    steps: number;
  }): Promise<void>;
  /**
   * Show the native Google Play Games achievements UI.
   * @since 0.1.0
   */
  showAchievements(): Promise<void>;
  /**
   * Submit a score to a leaderboard by its Play Console leaderboard id.
   * @since 0.1.0
   */
  submitScore(opts: {
    leaderboardId: string;
    score: number;
  }): Promise<void>;
  /**
   * Show the native UI for a single leaderboard.
   * @since 0.1.0
   */
  showLeaderboard(opts: {
    leaderboardId: string;
  }): Promise<void>;
  /**
   * Show the native all-leaderboards UI.
   * @since 0.1.0
   */
  showAllLeaderboards(): Promise<void>;
  /**
   * Load a saved-game snapshot by its stable name. Resolves `{ snapshot: null }`
   * when no snapshot exists for that name.
   * @since 0.1.0
   */
  loadSnapshot(opts: {
    name: string;
  }): Promise<{
    snapshot: Snapshot | null;
  }>;
  /**
   * Create or overwrite a saved-game snapshot. Conflicts are auto-resolved by
   * most-recently-modified (last write wins), with no merge.
   * @since 0.1.0
   */
  saveSnapshot(opts: {
    name: string;
    data: string;
    description?: string;
  }): Promise<void>;
  /**
   * List metadata for all of the player's snapshots.
   * @since 0.1.0
   */
  listSnapshots(): Promise<{
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
