import { WebPlugin } from "@capacitor/core";

import type {
  PlayerInfo,
  PlayGamesPlugin,
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

  async requestServerSideAccess(): Promise<{ authCode: string }> {
    return { authCode: "" };
  }

  async unlockAchievement(): Promise<void> {}

  async incrementAchievement(): Promise<void> {}

  async showAchievements(): Promise<void> {}

  async submitScore(): Promise<void> {}

  async showLeaderboard(): Promise<void> {}

  async showAllLeaderboards(): Promise<void> {}

  async loadSnapshot(): Promise<{ snapshot: Snapshot | null }> {
    return { snapshot: null };
  }

  async saveSnapshot(): Promise<void> {}

  async listSnapshots(): Promise<{ snapshots: SnapshotMeta[] }> {
    return { snapshots: [] };
  }

  async deleteSnapshot(): Promise<void> {}
}
