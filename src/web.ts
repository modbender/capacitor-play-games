import { WebPlugin } from "@capacitor/core";

import type {
  Achievement,
  FriendsResult,
  GameEventDefinition,
  Leaderboard,
  LeaderboardScore,
  LoadScoresResult,
  PlayerInfo,
  PlayerStats,
  PlayGamesPlugin,
  ScoreSubmissionResult,
  SignInResult,
  Snapshot,
  SnapshotMeta,
} from "./definitions";

/**
 * Web / non-native fallback: every method resolves to a safe default (signed
 * out, empty). Gate feature usage behind `isSignedIn()`, which is `false` here.
 */
export class PlayGamesWeb extends WebPlugin implements PlayGamesPlugin {
  async initialize(): Promise<void> {}

  async signIn(): Promise<SignInResult> {
    return { signedIn: false };
  }

  async isSignedIn(): Promise<{ signedIn: boolean }> {
    return { signedIn: false };
  }

  async getPlayer(): Promise<PlayerInfo> {
    return { playerId: "", displayName: "" };
  }

  async getPlayerId(): Promise<{ playerId: string }> {
    return { playerId: "" };
  }

  async loadPlayer(): Promise<{ player: PlayerInfo | null; stale: boolean }> {
    return { player: null, stale: false };
  }

  async loadFriends(): Promise<FriendsResult> {
    return { friends: [], stale: false, resolutionRequired: false };
  }

  async loadRecentlyPlayedWithPlayers(): Promise<{ players: PlayerInfo[]; stale: boolean }> {
    return { players: [], stale: false };
  }

  async showPlayerSearch(): Promise<{ player: PlayerInfo | null }> {
    return { player: null };
  }

  async showComparePlayer(): Promise<void> {}

  async requestServerSideAccess(): Promise<{ authCode: string }> {
    return { authCode: "" };
  }

  async unlockAchievement(): Promise<void> {}

  async revealAchievement(): Promise<void> {}

  async incrementAchievement(): Promise<{ unlocked: boolean }> {
    return { unlocked: false };
  }

  async setAchievementSteps(): Promise<{ unlocked: boolean }> {
    return { unlocked: false };
  }

  async loadAchievements(): Promise<{ achievements: Achievement[]; stale: boolean }> {
    return { achievements: [], stale: false };
  }

  async showAchievements(): Promise<void> {}

  async submitScore(): Promise<ScoreSubmissionResult> {
    return { leaderboardId: "", playerId: "", results: [] };
  }

  async showLeaderboard(): Promise<void> {}

  async showAllLeaderboards(): Promise<void> {}

  async loadLeaderboards(): Promise<{ leaderboards: Leaderboard[]; stale: boolean }> {
    return { leaderboards: [], stale: false };
  }

  async loadLeaderboard(): Promise<{ leaderboard: Leaderboard | null; stale: boolean }> {
    return { leaderboard: null, stale: false };
  }

  async loadTopScores(): Promise<LoadScoresResult> {
    return { leaderboard: null, scores: [], stale: false };
  }

  async loadPlayerCenteredScores(): Promise<LoadScoresResult> {
    return { leaderboard: null, scores: [], stale: false };
  }

  async loadCurrentPlayerScore(): Promise<{ score: LeaderboardScore | null; stale: boolean }> {
    return { score: null, stale: false };
  }

  async loadSnapshot(): Promise<{ snapshot: Snapshot | null }> {
    return { snapshot: null };
  }

  async saveSnapshot(): Promise<void> {}

  async listSnapshots(): Promise<{ snapshots: SnapshotMeta[] }> {
    return { snapshots: [] };
  }

  async deleteSnapshot(): Promise<void> {}

  async showSnapshots(): Promise<{ snapshot: SnapshotMeta | null; isNew: boolean }> {
    return { snapshot: null, isNew: false };
  }

  async getSnapshotLimits(): Promise<{ maxDataSize: number; maxCoverImageSize: number }> {
    return { maxDataSize: 0, maxCoverImageSize: 0 };
  }

  async recordGameEvent(): Promise<void> {}

  async recordGameEvents(): Promise<void> {}

  async recordProgressUpdate(): Promise<void> {}

  async requestGameEventsUpload(): Promise<void> {}

  async incrementEvent(): Promise<void> {}

  async loadEvents(): Promise<{ events: GameEventDefinition[]; stale: boolean }> {
    return { events: [], stale: false };
  }

  async loadEventsByIds(): Promise<{ events: GameEventDefinition[]; stale: boolean }> {
    return { events: [], stale: false };
  }

  async requestRecallAccess(): Promise<{ sessionId: string }> {
    return { sessionId: "" };
  }

  async loadPlayerStats(): Promise<{ stats: PlayerStats; stale: boolean }> {
    return { stats: {}, stale: false };
  }
}
