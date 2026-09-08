# @idleflowgames/capacitor-play-games

Capacitor 8 plugin for platform games services: **Google Play Games Services
(PGS v2)** on Android and **Apple GameKit / Game Center** on iOS, with a safe
no-op fallback on web. One TypeScript API covers sign-in, achievements,
leaderboards, and saved games across both platforms.

## Install

```bash
npm install @idleflowgames/capacitor-play-games
npx cap sync
```

## Supported platforms

| Platform | Backing API                                          | Notes                                                       |
| -------- | ---------------------------------------------------- | ----------------------------------------------------------- |
| Android  | Google Play Games Services v2 (`play-services-games-v2`) | Requires PGS configured in the Google Play Console.     |
| iOS      | Apple GameKit / Game Center                          | Requires the Game Center capability + App Store Connect setup. |
| Web      | none                                                 | Every method resolves to a safe default (signed out, empty). |

## Platform setup

Achievement and leaderboard **ids are yours**: the plugin takes opaque id strings
and passes them straight through to the platform. Create them in the Google Play
Console (Android) and App Store Connect / Game Center (iOS), then pass the
matching id at each call site. The two platforms issue different ids for the same
logical achievement, so keep a per-platform map in your app.

### Android

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

### iOS

Enable the **Game Center** capability on your app target in Xcode and create the
app's achievements / leaderboards in App Store Connect. The system presents the
Game Center sign-in UI.

> If your app shows an App Tracking Transparency (ATT) prompt, request it
> **before** calling `initialize()`. iOS suppresses an ATT prompt shown while
> Game Center's "Welcome back" banner is on screen, and `initialize()` is what
> can trigger that banner.

## Usage

```ts
import { PlayGames } from "@idleflowgames/capacitor-play-games";

await PlayGames.initialize();

const { signedIn } = await PlayGames.signIn(); // silent by default
if (!signedIn) {
  // Force the interactive flow from an explicit user gesture:
  await PlayGames.signIn({ silent: false });
}

await PlayGames.unlockAchievement({ id: platformAchievementId });
await PlayGames.submitScore({ leaderboardId: platformLeaderboardId, score: 1234 });

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
* [`fetchIdentityVerificationSignature()`](#fetchidentityverificationsignature)
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

Initialize the native games SDK. Idempotent.

On Android this triggers `PlayGamesSdk.initialize`; on iOS it installs the
GameKit authentication handler. Call once, after any App Tracking
Transparency prompt has resolved, before the other methods.

**Since:** 0.1.0

--------------------


### signIn(...)

```typescript
signIn(opts?: { silent?: boolean | undefined; } | undefined) => Promise<SignInResult>
```

Sign in to the platform games service.

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

On Android and iOS this rejects when no player is signed in. On web (the
no-op fallback) it resolves an empty profile (`playerId: ""`).

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

Android only. iOS rejects (unimplemented); the web fallback resolves an empty
`authCode`.

| Param      | Type                                                             |
| ---------- | ---------------------------------------------------------------- |
| **`opts`** | <code>{ serverClientId: string; forceRefresh?: boolean; }</code> |

**Returns:** <code>Promise&lt;{ authCode: string; }&gt;</code>

**Since:** 0.2.0

--------------------


### fetchIdentityVerificationSignature()

```typescript
fetchIdentityVerificationSignature() => Promise<IdentityVerificationSignature>
```

Fetch a GameKit identity-verification signature for the signed-in Game Center
player (`GKLocalPlayer.fetchItems(forIdentityVerificationSignature:)`). A
backend verifies the returned bundle against Apple's certificate to trust the
player id rather than the untrusted client's claim. Rejects when no player is
signed in.

iOS only. Android rejects (unimplemented); the web fallback resolves an empty
bundle.

**Returns:** <code>Promise&lt;<a href="#identityverificationsignature">IdentityVerificationSignature</a>&gt;</code>

**Since:** 0.2.0

--------------------


### unlockAchievement(...)

```typescript
unlockAchievement(opts: { id: string; }) => Promise<void>
```

Unlock an achievement by its platform id (Play Console achievement id on
Android, App Store Connect / Game Center id on iOS).

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

`steps` is interpreted differently per platform: on Android (PGS) it is a
discrete step count toward the achievement's Play Console step total; on
iOS (GameKit) it is added to `percentComplete` as percentage points.
Compute a platform-appropriate value (e.g. via `Capacitor.getPlatform()`)
so progress matches on both stores.

| Param      | Type                                        |
| ---------- | ------------------------------------------- |
| **`opts`** | <code>{ id: string; steps: number; }</code> |

**Since:** 0.1.0

--------------------


### showAchievements()

```typescript
showAchievements() => Promise<void>
```

Show the platform's native achievements UI.

**Since:** 0.1.0

--------------------


### submitScore(...)

```typescript
submitScore(opts: { leaderboardId: string; score: number; }) => Promise<void>
```

Submit a score to a leaderboard by its platform id.

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
player signing out of the platform service system-wide.

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
| **`playerId`**    | <code>string</code> | Stable, platform-assigned player id (PGS player id / GameKit `gamePlayerID`). |
| **`displayName`** | <code>string</code> | Display name as shown in Google Play Games / Game Center.                     |
| **`avatarUrl`**   | <code>string</code> | URL of the player's avatar image, when the platform exposes one.              |


#### IdentityVerificationSignature

A GameKit identity-verification bundle
(`GKLocalPlayer.fetchItems(forIdentityVerificationSignature:)`). A third-party
server verifies `signature` against the certificate at `publicKeyUrl` to trust
the Game Center `playerId` without relaying it through the untrusted client.

| Prop               | Type                | Description                                                                   |
| ------------------ | ------------------- | ----------------------------------------------------------------------------- |
| **`publicKeyUrl`** | <code>string</code> | URL of Apple's public-key certificate used to verify `signature`.             |
| **`signature`**    | <code>string</code> | Base64-encoded signature over the verification payload.                       |
| **`salt`**         | <code>string</code> | Base64-encoded random salt Apple mixed into the signed payload.               |
| **`timestamp`**    | <code>number</code> | Signature creation time, in epoch milliseconds (check freshness server-side). |
| **`playerId`**     | <code>string</code> | The player id the signature attests (GameKit `gamePlayerID`).                 |
| **`bundleId`**     | <code>string</code> | The app's bundle id, part of the signed payload.                              |
| **`teamPlayerId`** | <code>string</code> | GameKit `teamPlayerID` (stable across the team's games), when available.      |
| **`gamePlayerId`** | <code>string</code> | GameKit `gamePlayerID` (stable per game), when available.                     |


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
pnpm install
pnpm verify      # lint + typecheck + build + pack check
```

The TypeScript bridge is built to `dist/` (ESM + CJS + types). The native sources
under `android/` and `ios/` ship in the package and are wired up by `npx cap sync`.

## License

[MIT](./LICENSE) © Idle Flow Games
