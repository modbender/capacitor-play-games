# @modbender/capacitor-play-games

Capacitor 8 plugin for **Google Play Games Services (PGS v2)** on Android,
with a safe no-op fallback on web. One TypeScript API covers sign-in,
achievements, leaderboards, and saved games.

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
| -------- | -------------------------------------------------------- | ------------------------------------------------------------ |
| Android  | Google Play Games Services v2 (`play-services-games-v2`) | Requires PGS configured in the Google Play Console.          |
| Web      | none                                                     | Every method resolves to a safe default (signed out, empty). |

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
await PlayGames.submitScore({ leaderboardId, score: 1234 });

// Cross-device saves:
await PlayGames.saveSnapshot({ name: "main", data: JSON.stringify(state) });
const { snapshot } = await PlayGames.loadSnapshot({ name: "main" });

// React to system-driven sign-in changes (e.g. signed out via Settings):
await PlayGames.addListener("signInStateChanged", ({ signedIn }) => {
  // update UI
});
```

On web every method resolves to a safe default, so gate feature usage behind
`isSignedIn()` rather than platform checks.

## API

<docgen-index>

* [`initialize()`](#initialize)
* [`signIn(...)`](#signin)
* [`isSignedIn()`](#issignedin)
* [`getPlayer()`](#getplayer)
* [`requestServerSideAccess(...)`](#requestserversideaccess)
* [`unlockAchievement(...)`](#unlockachievement)
* [`incrementAchievement(...)`](#incrementachievement)
* [`showAchievements()`](#showachievements)
* [`submitScore(...)`](#submitscore)
* [`showLeaderboard(...)`](#showleaderboard)
* [`showAllLeaderboards()`](#showallleaderboards)
* [`loadSnapshot(...)`](#loadsnapshot)
* [`saveSnapshot(...)`](#savesnapshot)
* [`listSnapshots()`](#listsnapshots)
* [`deleteSnapshot(...)`](#deletesnapshot)
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
| ---------- | ---------------------------------- |
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


### requestServerSideAccess(...)

```typescript
requestServerSideAccess(opts: { serverClientId: string; forceRefresh?: boolean; }) => Promise<{ authCode: string; }>
```

Request a one-time OAuth 2.0 server auth code for the signed-in Play Games
player, for a backend to exchange for the AUTHORITATIVE player id (Google Play
Games Services v2 `GamesSignInClient.requestServerSideAccess`).

`serverClientId` is the OAuth 2.0 **web** client id backing the game; the code
is redeemed against it server-side. `forceRefresh` (default `false`) requests a
fresh code even if one was recently granted.

The web fallback resolves an empty `authCode`.

| Param      | Type                                                             |
| ---------- | ---------------------------------------------------------------- |
| **`opts`** | <code>{ serverClientId: string; forceRefresh?: boolean; }</code> |

**Returns:** <code>Promise&lt;{ authCode: string; }&gt;</code>

**Since:** 0.2.0

--------------------


### unlockAchievement(...)

```typescript
unlockAchievement(opts: { id: string; }) => Promise<void>
```

Unlock an achievement by its Play Console achievement id.

| Param      | Type                         |
| ---------- | ---------------------------- |
| **`opts`** | <code>{ id: string; }</code> |

**Since:** 0.1.0

--------------------


### incrementAchievement(...)

```typescript
incrementAchievement(opts: { id: string; steps: number; }) => Promise<void>
```

Increment a partial (incremental) achievement.

`steps` is a discrete step count toward the achievement's Play Console
step total, and must be greater than 0 — the call rejects otherwise.

| Param      | Type                                        |
| ---------- | ------------------------------------------- |
| **`opts`** | <code>{ id: string; steps: number; }</code> |

**Since:** 0.1.0

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
submitScore(opts: { leaderboardId: string; score: number; }) => Promise<void>
```

Submit a score to a leaderboard by its Play Console leaderboard id.

| Param      | Type                                                   |
| ---------- | ------------------------------------------------------ |
| **`opts`** | <code>{ leaderboardId: string; score: number; }</code> |

**Since:** 0.1.0

--------------------


### showLeaderboard(...)

```typescript
showLeaderboard(opts: { leaderboardId: string; }) => Promise<void>
```

Show the native UI for a single leaderboard.

| Param      | Type                                    |
| ---------- | --------------------------------------- |
| **`opts`** | <code>{ leaderboardId: string; }</code> |

**Since:** 0.1.0

--------------------


### showAllLeaderboards()

```typescript
showAllLeaderboards() => Promise<void>
```

Show the native all-leaderboards UI.

**Since:** 0.1.0

--------------------


### loadSnapshot(...)

```typescript
loadSnapshot(opts: { name: string; }) => Promise<{ snapshot: Snapshot | null; }>
```

Load a saved-game snapshot by its stable name. Resolves `{ snapshot: null }`
when no snapshot exists for that name.

| Param      | Type                           |
| ---------- | ------------------------------ |
| **`opts`** | <code>{ name: string; }</code> |

**Returns:** <code>Promise&lt;{ snapshot: <a href="#snapshot">Snapshot</a> | null; }&gt;</code>

**Since:** 0.1.0

--------------------


### saveSnapshot(...)

```typescript
saveSnapshot(opts: { name: string; data: string; description?: string; }) => Promise<void>
```

Create or overwrite a saved-game snapshot. Conflicts are auto-resolved by
most-recently-modified (last write wins), with no merge.

| Param      | Type                                                               |
| ---------- | ------------------------------------------------------------------ |
| **`opts`** | <code>{ name: string; data: string; description?: string; }</code> |

**Since:** 0.1.0

--------------------


### listSnapshots()

```typescript
listSnapshots() => Promise<{ snapshots: SnapshotMeta[]; }>
```

List metadata for all of the player's snapshots.

**Returns:** <code>Promise&lt;{ snapshots: SnapshotMeta[]; }&gt;</code>

**Since:** 0.1.0

--------------------


### deleteSnapshot(...)

```typescript
deleteSnapshot(opts: { name: string; }) => Promise<void>
```

Delete a saved-game snapshot by its stable name.

| Param      | Type                           |
| ---------- | ------------------------------ |
| **`opts`** | <code>{ name: string; }</code> |

**Since:** 0.1.0

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


#### SignInResult

Result of a sign-in attempt, or the payload of a sign-in state change.

| Prop           | Type                                              | Description                                               |
| -------------- | ------------------------------------------------- | --------------------------------------------------------- |
| **`signedIn`** | <code>boolean</code>                              | Whether the player is currently authenticated.            |
| **`player`**   | <code><a href="#playerinfo">PlayerInfo</a></code> | The player profile, present only when `signedIn` is true. |


#### PlayerInfo

A signed-in player's public profile.

| Prop              | Type                | Description                                                                   |
| ----------------- | ------------------- | ----------------------------------------------------------------------------- |
| **`playerId`**    | <code>string</code> | Stable, platform-assigned Play Games player id.                   |
| **`displayName`** | <code>string</code> | Display name as shown in Google Play Games.                       |
| **`avatarUrl`**   | <code>string</code> | URL of the player's avatar image, when the platform exposes one.  |


#### Snapshot

A saved-game snapshot together with its serialized payload.

| Prop              | Type                | Description                                                             |
| ----------------- | ------------------- | ----------------------------------------------------------------------- |
| **`name`**        | <code>string</code> | Stable unique name the snapshot was saved under.                        |
| **`description`** | <code>string</code> | Human-readable description stored with the snapshot.                    |
| **`modifiedAt`**  | <code>number</code> | Last-modified time, in epoch milliseconds.                              |
| **`data`**        | <code>string</code> | The serialized save payload as a UTF-8 string (encode binary yourself). |


#### SnapshotMeta

<a href="#snapshot">Snapshot</a> metadata without the payload, as returned by `listSnapshots`.

| Prop              | Type                | Description                                          |
| ----------------- | ------------------- | ---------------------------------------------------- |
| **`name`**        | <code>string</code> | Stable unique name of the snapshot.                  |
| **`description`** | <code>string</code> | Human-readable description stored with the snapshot. |
| **`modifiedAt`**  | <code>number</code> | Last-modified time, in epoch milliseconds.           |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


### Type Aliases


#### SignInStateChangedEvent

Payload of the `signInStateChanged` event.

<code><a href="#signinresult">SignInResult</a></code>

</docgen-api>

## Development

```bash
bun install
bun run verify   # typecheck + build
```

The TypeScript bridge is built to `dist/` (ESM + CJS + types). The native sources
under `android/` ship in the package and are wired up by `npx cap sync`.

## License

[MIT](./LICENSE) © Idle Flow Games (original), © modbender (fork)
