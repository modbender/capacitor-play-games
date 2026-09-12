# @modbender/capacitor-play-games

Capacitor 8 plugin binding the full **Google Play Games Services v2 (PGS v2)**
client surface on Android, with a safe no-op fallback on web. One TypeScript
API covers sign-in, players and friends, achievements, leaderboards, saved
games, game stats, legacy events, Recall, and player stats.

## About this fork

This is a fork of [`@idleflowgames/capacitor-play-games`](https://www.npmjs.com/package/@idleflowgames/capacitor-play-games)
0.2.1, MIT-licensed.

Upstream's GitHub repository — `github.com/idleflowgames/capacitor-play-games`,
the URL its own `package.json` still points at — returned 404 on 2026-09-08,
while the npm package remained published. The source here was recovered from
that published tarball rather than forked through GitHub. At recovery, the
Kotlin, Swift, Gradle and podspec files shipped in the tarball and were
vendored byte-for-byte. The TypeScript layer did **not** ship — the package
carried `dist/` only, and its sourcemaps set `sourcesContent: false` — so
`src/` here was reconstructed from `dist/esm/*.js` plus the emitted `.d.ts`.
The declarations retained every doc comment, which made that reconstruction
faithful rather than a rewrite.

That claim was checked rather than asserted, at 0.3.0, before any of the
changes below: building `src/` as reconstructed reproduced upstream's
published `dist/` exactly — all six emitted files, both the `.js` and the
`.d.ts`, were identical to the ones in the 0.2.1 tarball once formatting was
normalised. That is what established the reconstruction was faithful. It is a
historical checkpoint, not a standing guarantee — the 0.4.0 changes below
(iOS removed, in particular) mean this tree no longer reproduces upstream's
`dist/`, by design.

### Changes from upstream 0.2.1

- **`android/build.gradle` now applies the Kotlin Android plugin.** Upstream puts
  `kotlin-gradle-plugin` on the buildscript classpath and uses a
  `kotlin { compilerOptions { ... } }` block, but never applies the plugin, so
  Gradle rejects that block with `Could not find method kotlin()` before
  compiling any source. One line.
- Renamed to the `@modbender` scope.
- **iOS support removed entirely** (0.4.0): the `ios/` sources, `Package.swift`,
  `CapacitorPlayGames.podspec`, and `fetchIdentityVerificationSignature()`. The
  Swift was inherited from upstream and had never been compiled or run —
  nothing had been through Xcode. The package is now Android plus a safe web
  no-op fallback; see the changelog for the full rationale.
- **Bound the rest of the Play Games Services v2 client surface** (0.5.0): every
  one of `PlayGames`'s nine client factories now has at least one method behind
  it. See "What this plugin deliberately doesn't bind" below for what was
  considered and left out.

Deliberately unchanged: the Android namespace is still
`com.idleflowgames.playgames`. Keeping it means this tree diffs cleanly against
the 0.2.1 tarball, so a reviewer can confirm the Android delta is exactly the one
line above. Renaming it is a mechanical follow-up, not a blocker.

### Status

**Android is verified.** The plugin module compiles and assembles against a
real Capacitor 8 app — `:…-capacitor-play-games:assembleDebug` is BUILD
SUCCESSFUL on Gradle 8.14.3, Android Gradle Plugin 8.13.0 and JDK 21.

The Gradle fix is confirmed by reproduction rather than by reading: revert that
one line, rebuild, and the build fails with exactly the error upstream's open
issue reports —

```
Could not find method kotlin() for arguments [...] on project
':modbender-capacitor-play-games' of type org.gradle.api.Project.
```

Worth knowing: the module keeps upstream's own buildscript classpath pinning AGP
9.3.1 and Kotlin 2.4.10, which is *higher* than the consuming app's AGP 8.13.0.
That combination was expected to be a second conflict and is not — it resolves
and builds green as-is.

The TypeScript builds and typechecks clean on TypeScript 7.0.2.

The original MIT copyright is retained in [LICENSE](./LICENSE) alongside this
fork's.

## Install

```bash
bun add @modbender/capacitor-play-games
bunx cap sync
```

## Supported platforms

| Platform | Backing API                                              | Notes                                                        |
| -------- | --------------------------------------------------------- | ------------------------------------------------------------ |
| Android  | Google Play Games Services v2 (`play-services-games-v2`) | Requires PGS configured in the Google Play Console.          |
| Web      | none                                                       | Every method resolves to a safe default (signed out, empty). |

## Platform setup

Achievement and leaderboard **ids are yours**: the plugin takes opaque id strings
and passes them straight through to Play Games Services. Create them in the
Google Play Console, then pass the matching id at each call site.

Configure Play Games Services v2 in the Google Play Console, then wire your
project id into Android resources and the app manifest (see Google's
[Play Games Services docs](https://developer.android.com/games/pgs)):

```xml
<!-- android/app/src/main/res/values/games-ids.xml -->
<resources>
  <string name="game_services_project_id" translatable="false">YOUR_PGS_PROJECT_ID</string>
</resources>
```

```xml
<!-- inside <application> in android/app/src/main/AndroidManifest.xml -->
<meta-data
  android:name="com.google.android.gms.games.APP_ID"
  android:value="@string/game_services_project_id" />
```

`google-services.json` is not required for PGS v2 on its own; it is only needed
if you also wire Firebase.

### Publishing takes up to two hours to reach players

Play Games Services configuration — achievements, leaderboards, and the event
and game-stats definitions — is published independently of the app itself, and
publishing it does not publish the app. Changes take up to two hours to
propagate, so publish configuration changes at least two hours before a build
that depends on them reaches players, or sign-in and the features built on it
can fail. Before the configuration is published, only listed testers and
players on enabled release tracks can reach Play Games Services at all, which
is the usual explanation for a call that works for the developer and fails for
everyone else.

## Usage

```ts
import { PlayGames } from "@modbender/capacitor-play-games";

await PlayGames.initialize();

const { signedIn } = await PlayGames.signIn(); // silent by default
if (!signedIn) {
  // Force the interactive flow from an explicit user gesture:
  await PlayGames.signIn({ silent: false });
}

await PlayGames.unlockAchievement({ id: achievementId });
const { results } = await PlayGames.submitScore({ leaderboardId, score: 1234 });

// Cross-device saves:
await PlayGames.saveSnapshot({ name: "main", data: JSON.stringify(state) });
const { snapshot } = await PlayGames.loadSnapshot({ name: "main" });

// Game stats: fire-and-forget until you flush the queue.
await PlayGames.recordGameEvent({ name: "levelStart", properties: { level: { int: 4 } } });
await PlayGames.requestGameEventsUpload();

// React to system-driven sign-in changes (e.g. signed out via Settings):
await PlayGames.addListener("signInStateChanged", ({ signedIn }) => {
  // update UI
});
```

On web every method resolves to a safe default, so gate feature usage behind
`isSignedIn()` rather than platform checks.

## Limits

These are Google's documented limits, read from Play Games Services
documentation pages rather than from the SDK jar the way the rest of this
plugin's behaviour was — call them out as documented, not measured, since the
rest of this README is the other way round.

**Game stats** (`recordGameEvent` / `recordGameEvents` / `recordProgressUpdate`):
30 events per `recordGameEvents` batch, 25 properties per event, event names
and property keys up to 100 characters, string property values up to 1024
characters. All four are enforced natively; a call over any limit rejects
before it reaches the SDK.

**Achievements**: 400 achievements per game; 2,000 points total per game, at
most 200 points on any one achievement, and every point value a multiple of 5.
XP for an achievement is 100 times its point value. Name up to 100 characters,
description up to 500; names and descriptions must be unique within the game,
and a new achievement needs a unique icon. Of the three states, revealed is
the usual starting point and hidden is meant to be used sparingly.

**Leaderboards**: 70 leaderboards per game. A leaderboard's `scoreOrder` is
fixed once it's published in the Play Console and cannot change afterwards.
Daily scores reset at UTC-7, weekly scores reset at the Saturday/Sunday
midnight boundary UTC-7, and all-time scores never reset. Scores are stored as
long integers, so the raw number is not always the displayed one: a time
score is submitted in milliseconds and a currency score in millionths of the
main currency unit. Name up to 100 characters; Google's documentation doesn't
state a `scoreTag` length limit, so none is stated here either.

**Saved games**: a snapshot's data is capped at 3 MB and its cover image at
800 KB, per Google's Cloud Save guide. `getSnapshotLimits` returns the SDK's
own runtime figures rather than these constants, since a hardcoded value
would go stale if Google changed it; the unit those two accessors report is
almost certainly bytes but that is not confirmed by any reference this was
checked against, so verify the magnitude on a real device before comparing a
payload's length against them.

**Legacy events** (`incrementEvent` / `loadEvents` / `loadEventsByIds`): every
event is typed as either a premium-currency source or a premium-currency
sink, which is the clearest reason this feature isn't a substitute for game
stats. Name up to 100 characters, description up to 500. The SDK batches
increments, so the counts `loadEvents` returns are cumulative rather than
fine-grained; Google's documentation gives no maximum number of events per
game and no numeric rate limit, so neither is stated here.

## What this plugin deliberately doesn't bind

Each of these was checked against the v2 SDK jar rather than skipped by
oversight — a considered omission, not a gap waiting to be filled.

- **Multiplayer.** The v2 SDK doesn't have it. Its `multiplayer` package
  contains only `ParticipantEntity` and `realtime.RoomEntity` — no
  `RealTimeMultiplayerClient`, `TurnBasedMultiplayerClient`, or invitation/room
  API. Google removed multiplayer from Play Games Services v2 entirely; no
  plugin can bind what the SDK no longer exposes.
- **Video capture and game metadata.** `VideosClient` and `GamesMetadataClient`
  still ship as interfaces in the artifact, but `PlayGames` — the v2 entry
  point — has no factory method for either. Its nine static factories are the
  complete list, and neither of these two is on it, so both are unreachable
  from v2 code, not merely unbound.
- **`loadMoreFriends`, `loadMoreRecentlyPlayedWithPlayers`, `loadMoreScores`.**
  Each takes a live native `Buffer` object as its paging cursor, and that
  buffer can't survive a round trip to JS without a handle registry keeping it
  alive on the native side and a lifetime the JS caller could leak. That's a
  larger design than paging is worth, so `loadFriends`,
  `loadRecentlyPlayedWithPlayers`, `loadTopScores` and
  `loadPlayerCenteredScores` take `pageSize`/`maxResults` instead of exposing a
  cursor.
- **`SnapshotsClient.resolveConflict`.** Manual conflict resolution needs both
  conflicting `Snapshot` objects to survive the same round trip — the same
  handle-lifetime problem as paging. `loadSnapshot` and `saveSnapshot` instead
  expose the SDK's four automatic `RESOLUTION_POLICY_*` strategies via
  `conflictPolicy`, defaulting to most-recently-modified (last write wins).
- **Game stats `eventId` and `durationValue`.** Both exist in the Play Games
  REST API but not on `PlayerGameEvent.Builder`, measured directly off the
  22.0.0 jar: it has no `eventId` setter (its idempotency-key constructor is
  non-public) and no duration-typed `addProperty` overload. Neither can be
  exposed without a REST path this plugin doesn't have.

## API

<docgen-index>

* [`initialize()`](#initialize)
* [`signIn(...)`](#signin)
* [`isSignedIn()`](#issignedin)
* [`getPlayer()`](#getplayer)
* [`getPlayerId()`](#getplayerid)
* [`loadPlayer(...)`](#loadplayer)
* [`loadFriends(...)`](#loadfriends)
* [`loadRecentlyPlayedWithPlayers(...)`](#loadrecentlyplayedwithplayers)
* [`showPlayerSearch()`](#showplayersearch)
* [`showComparePlayer(...)`](#showcompareplayer)
* [`requestServerSideAccess(...)`](#requestserversideaccess)
* [`unlockAchievement(...)`](#unlockachievement)
* [`revealAchievement(...)`](#revealachievement)
* [`incrementAchievement(...)`](#incrementachievement)
* [`setAchievementSteps(...)`](#setachievementsteps)
* [`loadAchievements(...)`](#loadachievements)
* [`showAchievements()`](#showachievements)
* [`submitScore(...)`](#submitscore)
* [`showLeaderboard(...)`](#showleaderboard)
* [`showAllLeaderboards()`](#showallleaderboards)
* [`loadLeaderboards(...)`](#loadleaderboards)
* [`loadLeaderboard(...)`](#loadleaderboard)
* [`loadTopScores(...)`](#loadtopscores)
* [`loadPlayerCenteredScores(...)`](#loadplayercenteredscores)
* [`loadCurrentPlayerScore(...)`](#loadcurrentplayerscore)
* [`loadSnapshot(...)`](#loadsnapshot)
* [`saveSnapshot(...)`](#savesnapshot)
* [`listSnapshots(...)`](#listsnapshots)
* [`deleteSnapshot(...)`](#deletesnapshot)
* [`showSnapshots(...)`](#showsnapshots)
* [`getSnapshotLimits()`](#getsnapshotlimits)
* [`recordGameEvent(...)`](#recordgameevent)
* [`recordGameEvents(...)`](#recordgameevents)
* [`recordProgressUpdate(...)`](#recordprogressupdate)
* [`requestGameEventsUpload()`](#requestgameeventsupload)
* [`incrementEvent(...)`](#incrementevent)
* [`loadEvents(...)`](#loadevents)
* [`loadEventsByIds(...)`](#loadeventsbyids)
* [`requestRecallAccess()`](#requestrecallaccess)
* [`loadPlayerStats(...)`](#loadplayerstats)
* [`addListener('signInStateChanged', ...)`](#addlistenersigninstatechanged-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### initialize()

```typescript
initialize() => Promise<void>
```

No-op. `PlayGamesSdk.initialize` runs automatically when the plugin
loads, driven by the Capacitor bridge — this call exists only to keep
the API symmetric with the web fallback.

**Since:** 0.1.0

--------------------


### signIn(...)

```typescript
signIn(opts?: { silent?: boolean | undefined; } | undefined) => Promise<SignInResult>
```

Sign in to Google Play Games.

`silent` (default `true`) attempts auto sign-in with no UI; on most devices
this succeeds if the player has previously authenticated this game. Pass
`silent: false` to force the full interactive flow, and only in response to
an explicit user gesture.

| Param      | Type                               |
| ---------- | ----------------------------------- |
| **`opts`** | <code>{ silent?: boolean; }</code> |

**Returns:** <code>Promise&lt;<a href="#signinresult">SignInResult</a>&gt;</code>

**Since:** 0.1.0

--------------------


### isSignedIn()

```typescript
isSignedIn() => Promise<{ signedIn: boolean; }>
```

Whether a player is currently signed in.

**Returns:** <code>Promise&lt;{ signedIn: boolean; }&gt;</code>

**Since:** 0.1.0

--------------------


### getPlayer()

```typescript
getPlayer() => Promise<PlayerInfo>
```

Get the signed-in player's profile.

Rejects when no player is signed in. On web (the no-op fallback) it
resolves an empty profile (`playerId: ""`).

**Returns:** <code>Promise&lt;<a href="#playerinfo">PlayerInfo</a>&gt;</code>

**Since:** 0.1.0

--------------------


### getPlayerId()

```typescript
getPlayerId() => Promise<{ playerId: string; }>
```

Get the signed-in player's id without loading the rest of their profile.

The web fallback resolves an empty `playerId`.

**Returns:** <code>Promise&lt;{ playerId: string; }&gt;</code>

**Since:** 0.5.0

--------------------


### loadPlayer(...)

```typescript
loadPlayer(opts: { playerId: string; forceReload?: boolean | undefined; }) => Promise<{ player: PlayerInfo | null; stale: boolean; }>
```

Load another player's profile by their Play Games player id. Resolves
`{ player: null }` when the lookup can't find that player.

`forceReload` (default `false`) bypasses any cache the SDK is holding.
The web fallback resolves `{ player: null, stale: false }`.

| Param      | Type                                                        |
| ---------- | ------------------------------------------------------------ |
| **`opts`** | <code>{ playerId: string; forceReload?: boolean; }</code> |

**Returns:** <code>Promise&lt;{ player: <a href="#playerinfo">PlayerInfo</a> | null; stale: boolean; }&gt;</code>

**Since:** 0.5.0

--------------------


### loadFriends(...)

```typescript
loadFriends(opts?: { pageSize?: number | undefined; forceReload?: boolean | undefined; resolve?: boolean | undefined; } | undefined) => Promise<FriendsResult>
```

Load a page of the signed-in player's Play Games friends.

`pageSize` (default 25) bounds the page. `forceReload` (default `false`)
bypasses any cache the SDK is holding. `resolve` (default `false`)
launches the consent UI when friends-list access hasn't been granted yet;
without it, that case resolves `{ friends: [], resolutionRequired: true }`
rather than rejecting — reading the friends list never puts an account
dialog in front of the player unless you opt in with `resolve: true`.

| Param      | Type                                                                            |
| ---------- | -------------------------------------------------------------------------------- |
| **`opts`** | <code>{ pageSize?: number; forceReload?: boolean; resolve?: boolean; }</code> |

**Returns:** <code>Promise&lt;<a href="#friendsresult">FriendsResult</a>&gt;</code>

**Since:** 0.5.0

--------------------


### loadRecentlyPlayedWithPlayers(...)

```typescript
loadRecentlyPlayedWithPlayers(opts?: { pageSize?: number | undefined; forceReload?: boolean | undefined; } | undefined) => Promise<{ players: PlayerInfo[]; stale: boolean; }>
```

Load players the signed-in player has recently played a match against.

`pageSize` (default 25) bounds the page. `forceReload` (default `false`)
bypasses any cache the SDK is holding.

| Param      | Type                                                        |
| ---------- | ------------------------------------------------------------ |
| **`opts`** | <code>{ pageSize?: number; forceReload?: boolean; }</code> |

**Returns:** <code>Promise&lt;{ players: <a href="#playerinfo">PlayerInfo</a>[]; stale: boolean; }&gt;</code>

**Since:** 0.5.0

--------------------


### showPlayerSearch()

```typescript
showPlayerSearch() => Promise<{ player: PlayerInfo | null; }>
```

Show the native player search UI. Resolves `{ player: null }` when the
player cancels without picking anyone.

**Returns:** <code>Promise&lt;{ player: <a href="#playerinfo">PlayerInfo</a> | null; }&gt;</code>

**Since:** 0.5.0

--------------------


### showComparePlayer(...)

```typescript
showComparePlayer(opts: { playerId: string; otherPlayerInGameName?: string | undefined; currentPlayerInGameName?: string | undefined; }) => Promise<void>
```

Show the native UI comparing the signed-in player's profile against another
player's.

`otherPlayerInGameName`/`currentPlayerInGameName` are optional in-game
display name hints shown alongside each player's Play Games name; supply
both or neither.

| Param      | Type                                                                                            |
| ---------- | -------------------------------------------------------------------------------------------------- |
| **`opts`** | <code>{ playerId: string; otherPlayerInGameName?: string; currentPlayerInGameName?: string; }</code> |

**Since:** 0.5.0

--------------------


### requestServerSideAccess(...)

```typescript
requestServerSideAccess(opts: { serverClientId: string; forceRefresh?: boolean | undefined; scopes?: AuthScopeName[] | undefined; }) => Promise<{ authCode: string; grantedScopes?: AuthScopeName[] | undefined; }>
```

Request a one-time OAuth 2.0 server auth code for the signed-in Play Games
player, for a backend to exchange for the AUTHORITATIVE player id (Google Play
Games Services v2 `GamesSignInClient.requestServerSideAccess`).

`serverClientId` is the OAuth 2.0 **web** client id backing the game; the code
is redeemed against it server-side. `forceRefresh` (default `false`) requests a
fresh code even if one was recently granted. Passing `scopes` additionally
requests consent for those OAuth scopes; the result's `grantedScopes` is
present only when `scopes` was passed, and lists what the player actually
granted (which can be a subset of what was requested).

The web fallback resolves an empty `authCode`.

| Param      | Type                                                                                              |
| ---------- | ---------------------------------------------------------------------------------------------------- |
| **`opts`** | <code>{ serverClientId: string; forceRefresh?: boolean; scopes?: AuthScopeName[]; }</code> |

**Returns:** <code>Promise&lt;{ authCode: string; grantedScopes?: AuthScopeName[]; }&gt;</code>

**Since:** 0.2.0

--------------------


### unlockAchievement(...)

```typescript
unlockAchievement(opts: { id: string; }) => Promise<void>
```

Unlock an achievement by its Play Console achievement id.

Resolves only once the server has recorded the unlock (the SDK's
`unlockImmediate`).

| Param      | Type                         |
| ---------- | ---------------------------- |
| **`opts`** | <code>{ id: string; }</code> |

**Since:** 0.1.0

--------------------


### revealAchievement(...)

```typescript
revealAchievement(opts: { id: string; }) => Promise<void>
```

Reveal a hidden achievement without unlocking it.

Resolves only once the server has recorded the reveal (the SDK's
`revealImmediate`).

| Param      | Type                         |
| ---------- | ---------------------------- |
| **`opts`** | <code>{ id: string; }</code> |

**Since:** 0.5.0

--------------------


### incrementAchievement(...)

```typescript
incrementAchievement(opts: { id: string; steps: number; }) => Promise<{ unlocked: boolean; }>
```

Increment a partial (incremental) achievement.

`steps` is a discrete step count toward the achievement's Play Console
step total, and must be greater than 0 — the call rejects otherwise.

Resolves only once the server has recorded the increment (the SDK's
`incrementImmediate`). `unlocked` is `true` when this increment caused
the achievement to become fully unlocked.

| Param      | Type                                        |
| ---------- | ------------------------------------------- |
| **`opts`** | <code>{ id: string; steps: number; }</code> |

**Returns:** <code>Promise&lt;{ unlocked: boolean; }&gt;</code>

**Since:** 0.1.0

--------------------


### setAchievementSteps(...)

```typescript
setAchievementSteps(opts: { id: string; steps: number; }) => Promise<{ unlocked: boolean; }>
```

Set the absolute step count of an incremental achievement, rather than
incrementing it by a delta. `steps` must be greater than 0 — the call
rejects otherwise.

Resolves only once the server has recorded the new step count (the
SDK's `setStepsImmediate`). `unlocked` is `true` when this call caused
the achievement to become fully unlocked.

| Param      | Type                                        |
| ---------- | ------------------------------------------- |
| **`opts`** | <code>{ id: string; steps: number; }</code> |

**Returns:** <code>Promise&lt;{ unlocked: boolean; }&gt;</code>

**Since:** 0.5.0

--------------------


### loadAchievements(...)

```typescript
loadAchievements(opts?: ForceReloadOptions) => Promise<{ achievements: Achievement[]; stale: boolean; }>
```

Load the signed-in player's achievements, with their current unlock state
and (for incremental achievements) step progress.

`forceReload` (default `false`) bypasses any cache the SDK is holding.

| Param      | Type                                                                |
| ---------- | ---------------------------------------------------------------------- |
| **`opts`** | <code><a href="#forcereloadoptions">ForceReloadOptions</a></code> |

**Returns:** <code>Promise&lt;{ achievements: <a href="#achievement">Achievement</a>[]; stale: boolean; }&gt;</code>

**Since:** 0.5.0

--------------------


### showAchievements()

```typescript
showAchievements() => Promise<void>
```

Show the native Google Play Games achievements UI.

**Since:** 0.1.0

--------------------


### submitScore(...)

```typescript
submitScore(opts: { leaderboardId: string; score: number; scoreTag?: string | undefined; }) => Promise<ScoreSubmissionResult>
```

Submit a score to a leaderboard by its Play Console leaderboard id.

`scoreTag` is an optional opaque string (e.g. a replay id) stored
alongside the score and returned with it later.

Resolves only once the server has recorded the score (the SDK's
`submitScoreImmediate`); see `ScoreSubmissionResult` for the shape of
the result.

| Param      | Type                                                                |
| ---------- | ---------------------------------------------------------------------- |
| **`opts`** | <code>{ leaderboardId: string; score: number; scoreTag?: string; }</code> |

**Returns:** <code>Promise&lt;<a href="#scoresubmissionresult">ScoreSubmissionResult</a>&gt;</code>

**Since:** 0.1.0

--------------------


### showLeaderboard(...)

```typescript
showLeaderboard(opts: { leaderboardId: string; timeSpan?: LeaderboardTimeSpan | undefined; collection?: LeaderboardCollection | undefined; }) => Promise<void>
```

Show the native UI for a single leaderboard.

`timeSpan` and `collection` preselect which slice of the leaderboard opens;
both default to the SDK's own default view when omitted.

| Param      | Type                                                                                            |
| ---------- | -------------------------------------------------------------------------------------------------- |
| **`opts`** | <code>{ leaderboardId: string; timeSpan?: LeaderboardTimeSpan; collection?: LeaderboardCollection; }</code> |

**Since:** 0.1.0

--------------------


### showAllLeaderboards()

```typescript
showAllLeaderboards() => Promise<void>
```

Show the native all-leaderboards UI.

**Since:** 0.1.0

--------------------


### loadLeaderboards(...)

```typescript
loadLeaderboards(opts?: ForceReloadOptions) => Promise<{ leaderboards: Leaderboard[]; stale: boolean; }>
```

Load metadata for all of the game's leaderboards.

`forceReload` (default `false`) bypasses any cache the SDK is holding.

| Param      | Type                                                                |
| ---------- | ---------------------------------------------------------------------- |
| **`opts`** | <code><a href="#forcereloadoptions">ForceReloadOptions</a></code> |

**Returns:** <code>Promise&lt;{ leaderboards: <a href="#leaderboard">Leaderboard</a>[]; stale: boolean; }&gt;</code>

**Since:** 0.5.0

--------------------


### loadLeaderboard(...)

```typescript
loadLeaderboard(opts: { leaderboardId: string; forceReload?: boolean | undefined; }) => Promise<{ leaderboard: Leaderboard | null; stale: boolean; }>
```

Load metadata for a single leaderboard. Resolves `{ leaderboard: null }`
when no leaderboard exists for that id.

`forceReload` (default `false`) bypasses any cache the SDK is holding.

| Param      | Type                                                              |
| ---------- | -------------------------------------------------------------------- |
| **`opts`** | <code>{ leaderboardId: string; forceReload?: boolean; }</code> |

**Returns:** <code>Promise&lt;{ leaderboard: <a href="#leaderboard">Leaderboard</a> | null; stale: boolean; }&gt;</code>

**Since:** 0.5.0

--------------------


### loadTopScores(...)

```typescript
loadTopScores(opts: LoadScoresOptions) => Promise<LoadScoresResult>
```

Load the top scores on a leaderboard.

| Param      | Type                                                                |
| ---------- | ---------------------------------------------------------------------- |
| **`opts`** | <code><a href="#loadscoresoptions">LoadScoresOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#loadscoresresult">LoadScoresResult</a>&gt;</code>

**Since:** 0.5.0

--------------------


### loadPlayerCenteredScores(...)

```typescript
loadPlayerCenteredScores(opts: LoadScoresOptions) => Promise<LoadScoresResult>
```

Load the scores immediately above and below the signed-in player's own
score on a leaderboard.

| Param      | Type                                                                |
| ---------- | ---------------------------------------------------------------------- |
| **`opts`** | <code><a href="#loadscoresoptions">LoadScoresOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#loadscoresresult">LoadScoresResult</a>&gt;</code>

**Since:** 0.5.0

--------------------


### loadCurrentPlayerScore(...)

```typescript
loadCurrentPlayerScore(opts: { leaderboardId: string; timeSpan?: LeaderboardTimeSpan | undefined; collection?: LeaderboardCollection | undefined; }) => Promise<{ score: LeaderboardScore | null; stale: boolean; }>
```

Load the signed-in player's own score on a leaderboard. Resolves
`{ score: null }` when the player has no score in that time span/collection.

| Param      | Type                                                                                            |
| ---------- | -------------------------------------------------------------------------------------------------- |
| **`opts`** | <code>{ leaderboardId: string; timeSpan?: LeaderboardTimeSpan; collection?: LeaderboardCollection; }</code> |

**Returns:** <code>Promise&lt;{ score: <a href="#leaderboardscore">LeaderboardScore</a> | null; stale: boolean; }&gt;</code>

**Since:** 0.5.0

--------------------


### loadSnapshot(...)

```typescript
loadSnapshot(opts: { name: string; conflictPolicy?: SnapshotConflictPolicy | undefined; }) => Promise<{ snapshot: Snapshot | null; }>
```

Load a saved-game snapshot by its stable name. Resolves `{ snapshot: null }`
when no snapshot exists for that name.

`conflictPolicy` (default "mostRecentlyModified") picks the automatic
strategy used if this device and another wrote the snapshot before either
saw the other's write.

| Param      | Type                                                                       |
| ---------- | ----------------------------------------------------------------------------- |
| **`opts`** | <code>{ name: string; conflictPolicy?: SnapshotConflictPolicy; }</code> |

**Returns:** <code>Promise&lt;{ snapshot: <a href="#snapshot">Snapshot</a> | null; }&gt;</code>

**Since:** 0.1.0

--------------------


### saveSnapshot(...)

```typescript
saveSnapshot(opts: { name: string; data: string; description?: string | undefined; playedTimeMillis?: number | undefined; progressValue?: number | undefined; coverImage?: string | undefined; conflictPolicy?: SnapshotConflictPolicy | undefined; }) => Promise<void>
```

Create or overwrite a saved-game snapshot.

`playedTimeMillis` and `progressValue` are opaque numbers you define and
that surface in the native snapshot-picker UI; `coverImage` is a base64
PNG/JPEG with no `data:` URI prefix. `conflictPolicy` (default
"mostRecentlyModified") picks the automatic strategy used if this device
and another wrote the snapshot before either saw the other's write —
manual conflict resolution isn't exposed, see the README.

| Param      | Type                                                                                                                                                                    |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`opts`** | <code>{ name: string; data: string; description?: string; playedTimeMillis?: number; progressValue?: number; coverImage?: string; conflictPolicy?: SnapshotConflictPolicy; }</code> |

**Since:** 0.1.0

--------------------


### listSnapshots(...)

```typescript
listSnapshots(opts?: ForceReloadOptions) => Promise<{ snapshots: SnapshotMeta[]; }>
```

List metadata for all of the player's snapshots.

`forceReload` (default `false`) bypasses any cache the SDK is holding.

| Param      | Type                                                                |
| ---------- | ---------------------------------------------------------------------- |
| **`opts`** | <code><a href="#forcereloadoptions">ForceReloadOptions</a></code> |

**Returns:** <code>Promise&lt;{ snapshots: <a href="#snapshotmeta">SnapshotMeta</a>[]; }&gt;</code>

**Since:** 0.1.0

--------------------


### deleteSnapshot(...)

```typescript
deleteSnapshot(opts: { name: string; }) => Promise<void>
```

Delete a saved-game snapshot by its stable name.

| Param      | Type                           |
| ---------- | ------------------------------- |
| **`opts`** | <code>{ name: string; }</code> |

**Since:** 0.1.0

--------------------


### showSnapshots(...)

```typescript
showSnapshots(opts?: { title?: string | undefined; allowAdd?: boolean | undefined; allowDelete?: boolean | undefined; maxSnapshots?: number | undefined; } | undefined) => Promise<{ snapshot: SnapshotMeta | null; isNew: boolean; }>
```

Show the native saved-games UI for picking, and optionally creating or
deleting, a snapshot. Resolves `{ snapshot: null }` when the player
cancels without picking one.

`title` (default `"Saved games"`) labels the picker; `allowAdd` and
`allowDelete` (both default `true`) show or hide those actions;
`maxSnapshots` (default: no limit) caps how many existing snapshots are
listed.

| Param      | Type                                                                                                        |
| ---------- | ---------------------------------------------------------------------------------------------------------------- |
| **`opts`** | <code>{ title?: string; allowAdd?: boolean; allowDelete?: boolean; maxSnapshots?: number; }</code> |

**Returns:** <code>Promise&lt;{ snapshot: <a href="#snapshotmeta">SnapshotMeta</a> | null; isNew: boolean; }&gt;</code>

**Since:** 0.5.0

--------------------


### getSnapshotLimits()

```typescript
getSnapshotLimits() => Promise<{ maxDataSize: number; maxCoverImageSize: number; }>
```

Get the platform's per-snapshot size limits, read from the device at
call time rather than hardcoded, so they can't go stale if Google
changes them. See "Saved games" in the Limits section above for
Google's documented figures and an important caveat about the unit
these two numbers are in.

The web fallback resolves both as `0`. Check `isSignedIn()` before treating
either as a real limit, because comparing a payload length against a zero
reports every save as oversized.

**Returns:** <code>Promise&lt;{ maxDataSize: number; maxCoverImageSize: number; }&gt;</code>

**Since:** 0.5.0

--------------------


### recordGameEvent(...)

```typescript
recordGameEvent(opts: { name: string; properties?: GameEventProperties | undefined; }) => Promise<void>
```

Record one game-stats event, rejecting if it's over the limits in
"Game stats" in the Limits section above (event name length, property
count, property key/value length) — all enforced natively.

This call is fire-and-forget: the native SDK method returns no result, so
a resolved promise means the event was queued on the device, not that
Google has received it. Call `requestGameEventsUpload` to flush the queue;
anything not yet uploaded is lost if the app crashes first.

| Param      | Type                                                                    |
| ---------- | -------------------------------------------------------------------------- |
| **`opts`** | <code>{ name: string; properties?: GameEventProperties; }</code> |

**Since:** 0.5.0

--------------------


### recordGameEvents(...)

```typescript
recordGameEvents(opts: { events: GameEvent[]; }) => Promise<void>
```

Record a batch of game-stats events in one native call, subject to the
same per-event limits as `recordGameEvent` plus a limit on the batch
itself — see "Game stats" in the Limits section above.

Fire-and-forget for the same reason as `recordGameEvent`: a resolved
promise means the batch was queued, not delivered.

| Param      | Type                                                                    |
| ---------- | -------------------------------------------------------------------------- |
| **`opts`** | <code>{ events: <a href="#gameevent">GameEvent</a>[]; }</code> |

**Since:** 0.5.0

--------------------


### recordProgressUpdate(...)

```typescript
recordProgressUpdate(opts: { currentProgress: number; properties?: GameEventProperties | undefined; }) => Promise<void>
```

Record a game-stats progress-update event: shorthand for `recordGameEvent`
with the fixed event name `progressUpdate` and an int property
`currentProgress` set to this value, subject to the same limits (see
"Game stats" in the Limits section above).

Fire-and-forget for the same reason as `recordGameEvent`.

| Param      | Type                                                                                   |
| ---------- | ------------------------------------------------------------------------------------------- |
| **`opts`** | <code>{ currentProgress: number; properties?: GameEventProperties; }</code> |

**Since:** 0.5.0

--------------------


### requestGameEventsUpload()

```typescript
requestGameEventsUpload() => Promise<void>
```

Flush any game-stats events queued by `recordGameEvent`/`recordGameEvents`/
`recordProgressUpdate` to Google's servers. This is the only confirmation
point in the game-stats API; nothing else in it reports delivery.

**Since:** 0.5.0

--------------------


### incrementEvent(...)

```typescript
incrementEvent(opts: { eventId: string; amount: number; }) => Promise<void>
```

Increment a legacy Play Console *event* (not a game-stats event) by
`amount`. Like the native SDK method, this is fire-and-forget: a resolved
promise means the request was made, not that the server has recorded it.

| Param      | Type                                              |
| ---------- | ---------------------------------------------------- |
| **`opts`** | <code>{ eventId: string; amount: number; }</code> |

**Since:** 0.5.0

--------------------


### loadEvents(...)

```typescript
loadEvents(opts?: ForceReloadOptions) => Promise<{ events: GameEventDefinition[]; stale: boolean; }>
```

Load all of the game's legacy Play Console events and the signed-in
player's cumulative counts on them.

`forceReload` (default `false`) bypasses any cache the SDK is holding.

| Param      | Type                                                                |
| ---------- | ---------------------------------------------------------------------- |
| **`opts`** | <code><a href="#forcereloadoptions">ForceReloadOptions</a></code> |

**Returns:** <code>Promise&lt;{ events: <a href="#gameeventdefinition">GameEventDefinition</a>[]; stale: boolean; }&gt;</code>

**Since:** 0.5.0

--------------------


### loadEventsByIds(...)

```typescript
loadEventsByIds(opts: { eventIds: string[]; forceReload?: boolean | undefined; }) => Promise<{ events: GameEventDefinition[]; stale: boolean; }>
```

Load specific legacy Play Console events by id. Rejects if `eventIds` is
empty.

`forceReload` (default `false`) bypasses any cache the SDK is holding.

| Param      | Type                                                        |
| ---------- | ------------------------------------------------------------ |
| **`opts`** | <code>{ eventIds: string[]; forceReload?: boolean; }</code> |

**Returns:** <code>Promise&lt;{ events: <a href="#gameeventdefinition">GameEventDefinition</a>[]; stale: boolean; }&gt;</code>

**Since:** 0.5.0

--------------------


### requestRecallAccess()

```typescript
requestRecallAccess() => Promise<{ sessionId: string; }>
```

Request a Play Games Recall session id: an opaque identifier from
`RecallClient`, obtained without requiring the player to sign in to Play
Games first.

The web fallback resolves an empty `sessionId`; don't forward it to a
backend as if it were a real session identifier.

**Returns:** <code>Promise&lt;{ sessionId: string; }&gt;</code>

**Since:** 0.5.0

--------------------


### loadPlayerStats(...)

```typescript
loadPlayerStats(opts?: ForceReloadOptions) => Promise<{ stats: PlayerStats; stale: boolean; }>
```

Load Play Games' engagement and spend predictions for the signed-in
player.

`forceReload` (default `false`) bypasses any cache the SDK is holding.

| Param      | Type                                                                |
| ---------- | ---------------------------------------------------------------------- |
| **`opts`** | <code><a href="#forcereloadoptions">ForceReloadOptions</a></code> |

**Returns:** <code>Promise&lt;{ stats: <a href="#playerstats">PlayerStats</a>; stale: boolean; }&gt;</code>

**Since:** 0.5.0

--------------------


### addListener('signInStateChanged', ...)

```typescript
addListener(event: "signInStateChanged", listener: (e: SignInStateChangedEvent) => void) => Promise<PluginListenerHandle>
```

Listen for sign-in state changes: an interactive sign-in completing, or the
player signing out of Google Play Games system-wide.

| Param          | Type                                                                  |
| -------------- | --------------------------------------------------------------------- |
| **`event`**    | <code>'signInStateChanged'</code>                                     |
| **`listener`** | <code>(e: <a href="#signinresult">SignInResult</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 0.1.0

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Remove all listeners registered through this plugin.

**Since:** 0.1.0

--------------------


### Interfaces


#### PlayerInfo

A player's public profile.

| Prop                           | Type                                                        | Description                                                                    |
| ------------------------------- | ------------------------------------------------------------ | -------------------------------------------------------------------------------- |
| **`playerId`**                 | <code>string</code>                                        | Stable, platform-assigned Play Games player id.                               |
| **`displayName`**              | <code>string</code>                                        | Display name as shown in Google Play Games.                                   |
| **`avatarUrl`**                | <code>string</code>                                        | Small avatar URL, absent when the player has no icon image.                   |
| **`hiResImageUrl`**            | <code>string</code>                                        | Full-resolution avatar URL, absent when the player has no hi-res image.       |
| **`bannerImageLandscapeUrl`**  | <code>string</code>                                        | Landscape profile banner URL, absent when the player has none.                |
| **`bannerImagePortraitUrl`**   | <code>string</code>                                        | Portrait profile banner URL, absent when the player has none.                 |
| **`title`**                    | <code>string</code>                                        | Play Games title such as "Expert", absent when unset.                         |
| **`retrievedAt`**              | <code>number</code>                                        | Epoch ms this profile was fetched, absent when the SDK doesn't report one.    |
| **`lastPlayedWithAt`**         | <code>number</code>                                        | Epoch ms this player was last played with, absent when unknown.               |
| **`level`**                    | <code><a href="#playerlevelinfo">PlayerLevelInfo</a></code> | Player level and XP progress, absent when the game has no level configuration. |
| **`friendStatus`**             | <code><a href="#playerfriendstatus">PlayerFriendStatus</a></code> | Relationship to the signed-in player, absent when unknown.              |
| **`friendsListVisibility`**    | <code><a href="#friendslistvisibility">FriendsListVisibility</a></code> | Present only on the signed-in player's own profile.               |


#### PlayerLevelInfo

A player's level and XP progress within the game's configured level ladder.

| Prop                | Type                                                | Description                                                                |
| -------------------- | ------------------------------------------------------ | ----------------------------------------------------------------------------- |
| **`currentXpTotal`** | <code>number</code>                                   | Absent when the SDK reports its CURRENT_XP_UNKNOWN sentinel.               |
| **`lastLevelUpAt`**  | <code>number</code>                                   | Epoch ms of the player's most recent level-up, absent when they haven't leveled up. |
| **`isMaxLevel`**     | <code>boolean</code>                                  | Whether the player has reached the highest configured level.              |
| **`currentLevel`**   | <code><a href="#playerlevel">PlayerLevel</a></code> | The level the player is currently in.                                     |
| **`nextLevel`**      | <code><a href="#playerlevel">PlayerLevel</a></code> | The next level up; equal to `currentLevel` when `isMaxLevel` is true.      |


#### PlayerLevel

One level's XP boundaries in the game's configured level ladder.

| Prop               | Type                 |
| ------------------- | ---------------------- |
| **`levelNumber`**  | <code>number</code>  |
| **`minXp`**        | <code>number</code>  |
| **`maxXp`**        | <code>number</code>  |


#### SignInResult

Result of a sign-in attempt, or the payload of a sign-in state change.

| Prop           | Type                                              | Description                                               |
| -------------- | -------------------------------------------------- | ----------------------------------------------------------- |
| **`signedIn`** | <code>boolean</code>                              | Whether the player is currently authenticated.            |
| **`player`**   | <code><a href="#playerinfo">PlayerInfo</a></code> | The player profile, present only when `signedIn` is true. |


#### FriendsResult

Result of `loadFriends`.

| Prop                     | Type                              | Description                                                                                                                       |
| ------------------------- | ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| **`friends`**            | <code>PlayerInfo[]</code>          | The requested page of friends, empty when `resolutionRequired` is true.                                                          |
| **`stale`**              | <code>boolean</code>               | Whether this result may already be out of date on the server.                                                                    |
| **`resolutionRequired`** | <code>boolean</code>               | True when the player has not granted friends-list access. `friends` is empty; call `loadFriends` again with `resolve: true` to show the consent UI. |


#### StandardAchievement

A single-unlock achievement's Play Console metadata plus the signed-in player's progress on it.

| Prop                   | Type                                                          | Description                                                        |
| ----------------------- | ---------------------------------------------------------------- | ---------------------------------------------------------------------- |
| **`id`**               | <code>string</code>                                             | Play Console achievement id.                                       |
| **`type`**             | <code>'standard'</code>                                         |                                                                      |
| **`name`**             | <code>string</code>                                             |                                                                      |
| **`description`**      | <code>string</code>                                             |                                                                      |
| **`state`**            | <code><a href="#achievementstate">AchievementState</a></code>  |                                                                      |
| **`xpValue`**          | <code>number</code>                                             | XP granted by unlocking this achievement.                          |
| **`lastUpdatedAt`**    | <code>number</code>                                             | Epoch ms this achievement's state last changed.                    |
| **`unlockedImageUrl`** | <code>string</code>                                             | Icon shown once unlocked, absent when the SDK reports none.        |
| **`revealedImageUrl`** | <code>string</code>                                             | Icon shown while hidden or revealed, absent when the SDK reports none. |


#### IncrementalAchievement

A stepped-counter achievement's Play Console metadata plus the signed-in player's step progress on it.

| Prop                        | Type                                                          | Description                                                        |
| ---------------------------- | ---------------------------------------------------------------- | ---------------------------------------------------------------------- |
| **`id`**                    | <code>string</code>                                             | Play Console achievement id.                                       |
| **`type`**                  | <code>'incremental'</code>                                      |                                                                      |
| **`name`**                  | <code>string</code>                                             |                                                                      |
| **`description`**           | <code>string</code>                                             |                                                                      |
| **`state`**                 | <code><a href="#achievementstate">AchievementState</a></code>  |                                                                      |
| **`xpValue`**               | <code>number</code>                                             | XP granted by fully unlocking this achievement.                    |
| **`lastUpdatedAt`**         | <code>number</code>                                             | Epoch ms this achievement's state last changed.                    |
| **`unlockedImageUrl`**      | <code>string</code>                                             | Icon shown once unlocked, absent when the SDK reports none.        |
| **`revealedImageUrl`**      | <code>string</code>                                             | Icon shown while hidden or revealed, absent when the SDK reports none. |
| **`currentSteps`**          | <code>number</code>                                             |                                                                      |
| **`totalSteps`**            | <code>number</code>                                             |                                                                      |
| **`formattedCurrentSteps`** | <code>string</code>                                             |                                                                      |
| **`formattedTotalSteps`**   | <code>string</code>                                             |                                                                      |


#### Leaderboard

A leaderboard's Play Console metadata.

| Prop              | Type                                                                | Description                                                                             |
| ------------------ | ---------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| **`leaderboardId`**| <code>string</code>                                                   |                                                                                          |
| **`displayName`**  | <code>string</code>                                                   |                                                                                          |
| **`iconImageUrl`** | <code>string</code>                                                   | Absent when the SDK reports none.                                                       |
| **`scoreOrder`**   | <code><a href="#leaderboardscoreorder">LeaderboardScoreOrder</a></code> | Play Console policy, not an SDK guarantee — see "Leaderboards" in the Limits section above. |
| **`variants`**     | <code>LeaderboardVariant[]</code>                                     | One entry per time-span/collection combination the SDK reports.                        |


#### LeaderboardVariant

One time-span/collection slice of a leaderboard, with the signed-in player's standing in it.

| Prop                    | Type                                                              | Description                                                       |
| ------------------------ | -------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| **`timeSpan`**          | <code><a href="#leaderboardtimespan">LeaderboardTimeSpan</a></code> |                                                                     |
| **`collection`**        | <code><a href="#leaderboardcollection">LeaderboardCollection</a></code> |                                                                 |
| **`hasPlayerInfo`**     | <code>boolean</code>                                                | Whether the signed-in player has a score in this variant.         |
| **`playerScore`**       | <code>number</code>                                                 | Absent when the SDK reports its PLAYER_SCORE_UNKNOWN sentinel.    |
| **`displayPlayerScore`**| <code>string</code>                                                 | Absent under the same condition as `playerScore`.                 |
| **`playerRank`**        | <code>number</code>                                                 | Absent when the SDK reports its PLAYER_RANK_UNKNOWN sentinel.     |
| **`displayPlayerRank`** | <code>string</code>                                                 | Absent under the same condition as `playerRank`.                  |
| **`playerScoreTag`**    | <code>string</code>                                                 | Absent when the signed-in player has no score tag in this variant.|
| **`numScores`**         | <code>number</code>                                                 | Absent when the SDK reports its NUM_SCORES_UNKNOWN sentinel.      |


#### LeaderboardScore

One entry in a leaderboard's score list.

| Prop                          | Type                                              | Description                                              |
| ------------------------------ | ---------------------------------------------------- | ------------------------------------------------------------ |
| **`rank`**                    | <code>number</code>                                | Absent when the SDK reports its LEADERBOARD_RANK_UNKNOWN sentinel. |
| **`displayRank`**             | <code>string</code>                                |                                                            |
| **`rawScore`**                | <code>number</code>                                |                                                            |
| **`displayScore`**            | <code>string</code>                                |                                                            |
| **`achievedAt`**              | <code>number</code>                                | Epoch ms this score was recorded.                         |
| **`scoreTag`**                | <code>string</code>                                |                                                            |
| **`scoreHolder`**             | <code><a href="#playerinfo">PlayerInfo</a></code> | The player who holds this score, absent when the SDK doesn't attach one. |
| **`scoreHolderDisplayName`**  | <code>string</code>                                |                                                            |
| **`scoreHolderIconImageUrl`** | <code>string</code>                                | Absent when the score holder has no icon image.           |
| **`scoreHolderHiResImageUrl`**| <code>string</code>                                | Absent when the score holder has no hi-res image.         |


#### LoadScoresOptions

Options shared by `loadTopScores` and `loadPlayerCenteredScores`.

| Prop               | Type                                                                | Description                                        |
| ------------------- | ---------------------------------------------------------------------- | ------------------------------------------------------ |
| **`leaderboardId`**| <code>string</code>                                                   |                                                     |
| **`timeSpan`**     | <code><a href="#leaderboardtimespan">LeaderboardTimeSpan</a></code>    | Defaults to "allTime".                             |
| **`collection`**   | <code><a href="#leaderboardcollection">LeaderboardCollection</a></code>| Defaults to "public".                              |
| **`maxResults`**   | <code>number</code>                                                   | Defaults to 25. Values outside 1-25 are not validated by this plugin, and whether the SDK clamps or rejects an out-of-range value was not confirmed. |
| **`forceReload`**  | <code>boolean</code>                                                  | Defaults to `false`.                               |


#### ForceReloadOptions

Options accepted by read methods whose only option is bypassing the SDK's cache.

| Prop              | Type                  | Description           |
| ------------------ | ----------------------- | ------------------------ |
| **`forceReload`** | <code>boolean</code>  | Defaults to `false`.  |


#### LoadScoresResult

Result of `loadTopScores` and `loadPlayerCenteredScores`.

| Prop              | Type                                                | Description                                              |
| ------------------ | ------------------------------------------------------ | ------------------------------------------------------------ |
| **`leaderboard`** | <code><a href="#leaderboard">Leaderboard</a> \| null</code> | The leaderboard's own metadata, absent when the SDK doesn't attach it. |
| **`scores`**      | <code>LeaderboardScore[]</code>                       |                                                            |
| **`stale`**       | <code>boolean</code>                                  |                                                            |


#### ScoreSubmissionResult

Result of a leaderboard score submission.

| Prop               | Type                                                                                                                                        | Description                          |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------- |
| **`leaderboardId`**| <code>string</code>                                                                                                                        |                                       |
| **`playerId`**     | <code>string</code>                                                                                                                        |                                       |
| **`results`**      | <code>{ timeSpan: LeaderboardTimeSpan; rawScore: number; formattedScore: string; scoreTag: string; newBest: boolean; }[]</code>            | One entry per time span the submission affected. The SDK does not guarantee all three (daily/weekly/all-time) are present, and does not report why a given one might be missing. |


#### SnapshotMeta

Snapshot metadata without the payload, as returned by `listSnapshots` and `showSnapshots`.

| Prop                        | Type                | Description                                                              |
| ---------------------------- | --------------------- | ----------------------------------------------------------------------------- |
| **`name`**                  | <code>string</code>  | Stable unique name the snapshot was saved under.                        |
| **`snapshotId`**            | <code>string</code>  | Platform-assigned snapshot id, distinct from `name`.                    |
| **`description`**           | <code>string</code>  |                                                                          |
| **`modifiedAt`**            | <code>number</code>  | Last-modified time, in epoch milliseconds.                              |
| **`playedTimeMillis`**      | <code>number</code>  | Absent when the SDK reports its PLAYED_TIME_UNKNOWN sentinel.           |
| **`progressValue`**         | <code>number</code>  | Absent when the SDK reports its PROGRESS_VALUE_UNKNOWN sentinel.        |
| **`deviceName`**            | <code>string</code>  | Name of the device the snapshot was last written from, absent when unknown. |
| **`coverImageUrl`**         | <code>string</code>  | Absent when the snapshot has no cover image.                            |
| **`coverImageAspectRatio`**| <code>number</code>  | Absent under the same condition as `coverImageUrl`.                     |
| **`hasChangePending`**      | <code>boolean</code> | Whether a change to this snapshot is pending sync to Google's servers.  |


#### Snapshot

A saved-game snapshot together with its serialized payload.

| Prop       | Type                | Description                                                             |
| ----------- | --------------------- | ----------------------------------------------------------------------- |
| **`data`** | <code>string</code>  | The serialized save payload as a UTF-8 string (encode binary yourself). |

<a href="#snapshot">Snapshot</a> extends <a href="#snapshotmeta">SnapshotMeta</a> — see that entry for `name`, `snapshotId`, `description`, `modifiedAt`, `playedTimeMillis`, `progressValue`, `deviceName`, `coverImageUrl`, `coverImageAspectRatio` and `hasChangePending`.


#### GameEvent

One game-stats event to record.

| Prop             | Type                                                              | Description |
| ----------------- | -------------------------------------------------------------------- | ------------ |
| **`name`**       | <code>string</code>                                                 |              |
| **`properties`** | <code><a href="#gameeventproperties">GameEventProperties</a></code>|              |


#### GameEventDefinition

A Play Console *event* resource — the legacy events feature, distinct from game stats.

| Prop                | Type                | Description                                          |
| -------------------- | --------------------- | --------------------------------------------------------- |
| **`eventId`**       | <code>string</code>  |                                                       |
| **`name`**          | <code>string</code>  |                                                       |
| **`description`**   | <code>string</code>  |                                                       |
| **`iconImageUrl`**  | <code>string</code>  | Absent when the SDK reports none.                    |
| **`value`**         | <code>number</code>  | Cumulative count the SDK has recorded for this event. |
| **`formattedValue`**| <code>string</code>  |                                                       |
| **`visible`**       | <code>boolean</code> |                                                       |


#### PlayerStats

Player-level engagement and spend predictions from Play Games Services.
Every field is absent when the SDK reports its UNSET_VALUE sentinel.

| Prop                          | Type                 | Description                                                    |
| ------------------------------ | ---------------------- | ------------------------------------------------------------------- |
| **`averageSessionLength`**    | <code>number</code>  |                                                                 |
| **`churnProbability`**        | <code>number</code>  |                                                                 |
| **`daysSinceLastPlayed`**     | <code>number</code>  |                                                                 |
| **`numberOfPurchases`**       | <code>number</code>  |                                                                 |
| **`numberOfSessions`**        | <code>number</code>  |                                                                 |
| **`sessionPercentile`**       | <code>number</code>  |                                                                 |
| **`spendPercentile`**         | <code>number</code>  |                                                                 |
| **`spendProbability`**        | <code>number</code>  |                                                                 |
| **`highSpenderProbability`**  | <code>number</code>  |                                                                 |
| **`totalSpendNext28Days`**    | <code>number</code>  |                                                                 |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


### Type Aliases


#### PlayerFriendStatus

A player's relationship to the signed-in player.

<code>"unknown" | "noRelationship" | "friend"</code>


#### FriendsListVisibility

Whether the signed-in player's friends list is visible to this game.

<code>"unknown" | "visible" | "requestRequired" | "featureUnavailable"</code>


#### AchievementState

Unlock state of an achievement.

<code>"unlocked" | "revealed" | "hidden"</code>


#### Achievement

An achievement's Play Console metadata plus the signed-in player's progress
on it. Narrow on `type` to reach the incremental-only step fields — they
don't exist on a standard achievement rather than being merely absent.

<code><a href="#standardachievement">StandardAchievement</a> | <a href="#incrementalachievement">IncrementalAchievement</a></code>


#### AchievementType

Whether an achievement is a single unlock or a stepped counter.

<code>"standard" | "incremental"</code>


#### LeaderboardTimeSpan

Which score window a leaderboard read or write applies to.

<code>"daily" | "weekly" | "allTime"</code>


#### LeaderboardCollection

Which scope of players a leaderboard read applies to.

<code>"public" | "friends"</code>


#### LeaderboardScoreOrder

Whether a lower or higher raw score ranks better on a leaderboard.

<code>"smallerIsBetter" | "largerIsBetter"</code>


#### SnapshotConflictPolicy

Automatic saved-game conflict resolution strategy, applied when two devices
wrote the same snapshot before either saw the other's write. Manual
conflict resolution (inspecting both copies in JS) isn't exposed — see
"What this plugin deliberately doesn't bind" above — so one of these always
applies.

<code>"mostRecentlyModified" | "longestPlaytime" | "lastKnownGood" | "highestProgress"</code>


#### GameEventValue

One property on a game-stats event. The four shapes map one-to-one onto the
four `PlayerGameEvent.Builder.addProperty` overloads; the tag is required
because a Play Console stat aggregates by the property's declared type and
guessing int-vs-double from a JS number would silently pick the wrong one.

`int` values are passed through as a 64-bit native long; values outside
`Number.MAX_SAFE_INTEGER` will lose precision crossing the JS/native bridge.

<code>{ int: number; double?: never; string?: never; bool?: never; } | { double: number; int?: never; string?: never; bool?: never; } | { string: string; int?: never; double?: never; bool?: never; } | { bool: boolean; int?: never; double?: never; string?: never; }</code>


#### GameEventProperties

Named properties attached to a game-stats event, keyed by property key.

<code><a href="#record">Record</a>&lt;string, <a href="#gameeventvalue">GameEventValue</a>&gt;</code>


#### AuthScopeName

An OAuth 2.0 scope that can be requested alongside a server auth code.

<code>"email" | "profile" | "openId"</code>


#### SignInStateChangedEvent

Payload of the `signInStateChanged` event.

<code><a href="#signinresult">SignInResult</a></code>

</docgen-api>

## Development

```bash
bun install
bun run verify   # typecheck + check-docs + build
```

`check-docs` is what gates publishing this README against `src/definitions.ts`
drifting apart — see `scripts/check-docs.ts`. The TypeScript bridge is built to
`dist/` (ESM + CJS + types). The native sources under `android/` ship in the
package and are wired up by `bunx cap sync`.

## License

[MIT](./LICENSE) © Idle Flow Games (original), © modbender (fork)
