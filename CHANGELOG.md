# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0] - 2026-09-11

Binds the rest of the Play Games Services v2 client surface: every one of
`PlayGames`'s nine client factories now has at least one method behind it,
measured directly out of `play-services-games-v2-22.0.0-api.jar` with
`javap`. See "What this plugin deliberately doesn't bind" in the README for
the surface that was considered and left out, and why.

### Added

- Players: `getPlayerId`, `loadPlayer`, `loadFriends`,
  `loadRecentlyPlayedWithPlayers`, `showPlayerSearch`, `showComparePlayer`.
  `getPlayer`'s `PlayerInfo` result gains hi-res and banner image URLs, title,
  level/XP, friend status and friends-list visibility.
- Achievements: `revealAchievement`, `setAchievementSteps`,
  `loadAchievements`.
- Leaderboards: `loadLeaderboards`, `loadLeaderboard`, `loadTopScores`,
  `loadPlayerCenteredScores`, `loadCurrentPlayerScore`. `showLeaderboard`
  gains `timeSpan`/`collection` to preselect which slice of the leaderboard
  opens.
- Saved games: `showSnapshots`, `getSnapshotLimits`. `loadSnapshot` and
  `saveSnapshot` gain `conflictPolicy`, one of the SDK's four automatic
  resolution strategies; `saveSnapshot` also gains `playedTimeMillis`,
  `progressValue` and `coverImage`. Snapshot metadata now carries
  `snapshotId`, played time, progress, device name and cover image
  alongside the existing fields.
- Game stats (`GameStatsClient`, new in the 22.0.0 SDK): `recordGameEvent`,
  `recordGameEvents`, `recordProgressUpdate`, `requestGameEventsUpload`.
- Legacy Play Console events (`EventsClient`): `incrementEvent`,
  `loadEvents`, `loadEventsByIds`.
- Recall (`RecallClient`): `requestRecallAccess`.
- Player stats (`PlayerStatsClient`): `loadPlayerStats`.
- `requestServerSideAccess` gains `scopes`, to additionally request OAuth
  scope consent and get back which scopes were actually granted.
- Every new method has a safe no-op web fallback, matching the existing
  methods' convention.

### Changed

- **`unlockAchievement`, `incrementAchievement` and `submitScore` now
  resolve only after the server has recorded the write**, using the SDK's
  `*Immediate` variants instead of their fire-and-forget counterparts. In
  0.4.0 and earlier these promises resolved before the server had the
  write; a caller that timed these calls, or relied on the old
  near-instant resolution, will see them take noticeably longer now.
  `incrementAchievement` also gains an `{ unlocked: boolean }` result, and
  `submitScore` gains a `scoreTag` option and a `ScoreSubmissionResult`
  result in place of `void`.
- **`loadSnapshot` now actually resolves `{ snapshot: null }` for a name with
  no save**, instead of manufacturing and returning an empty snapshot. The
  native call previously passed `createIfNotFound = true`, so a missing save
  was silently created rather than reported, making the null-result behaviour
  this method has documented since 0.1.0 unreachable in practice; it now
  passes `false` and recognises the platform's not-found status specifically.
  This aligns the implementation with the documented contract rather than
  changing that contract, but it is still a breaking behaviour change for a
  caller that assumed a non-null result: that assumption now needs a null
  check, and reading a save that doesn't exist no longer leaves an empty
  snapshot in the player's saved-games list as a side effect.
- Game stats recording (`recordGameEvent`/`recordGameEvents`/
  `recordProgressUpdate`) is the opposite: the native SDK methods return no
  result at all, so these resolve as soon as the event is queued on the
  device, not once Google has it. `requestGameEventsUpload` is the only
  confirmation point in that API.
- `package.json` `description` and `keywords` to cover the full surface
  rather than the original sign-in/achievements/leaderboards/saved-games
  set.

## [0.4.0] - 2026-09-08

### Removed

- iOS support, entirely: the `ios/` sources, `Package.swift`,
  `CapacitorPlayGames.podspec`, and `fetchIdentityVerificationSignature()` (the
  GameKit identity-verification bundle it returned) along with the
  `IdentityVerificationSignature` interface. The Swift was inherited from
  upstream and had never been compiled or run — nothing had been through
  Xcode — so this deletes no working functionality. The package is now
  Android plus a safe web no-op fallback.

### Changed

- `package.json` `description` no longer claims iOS support.

## [0.3.0] - 2026-09-08

Forked from `@idleflowgames/capacitor-play-games` 0.2.1. Upstream's GitHub
repository 404s while its npm package remains published, so the source was
recovered from the published tarball; see "About this fork" in the README for
what that recovery involved.

### Fixed

- Android: apply the Kotlin Android plugin. The build script declared
  `kotlin-gradle-plugin` on the buildscript classpath and used a
  `kotlin { compilerOptions { ... } }` block without ever applying the plugin,
  so Gradle failed with `Could not find method kotlin()` before compiling any
  source. Verified by reproduction: with the line reverted the build fails with
  that exact error, and with it applied `assembleDebug` succeeds against a
  Capacitor 8 app on Gradle 8.14.3 / AGP 8.13.0 / JDK 21.

### Changed

- Renamed to `@modbender/capacitor-play-games`, and the SwiftPM product from
  `IdleflowgamesCapacitorPlayGames` to `ModbenderCapacitorPlayGames` to match —
  Capacitor derives the product name from the full scoped package name.
- Toolchain: pnpm to bun; dropped the Biome and SwiftLint dev dependencies
  rather than ship lint configuration that has never been run here.

### Unchanged

- The Android namespace stays `com.idleflowgames.playgames`, so this tree diffs
  cleanly against the 0.2.1 tarball.

## [0.2.1] - 2026-07-30

### Changed

- Android: Play Games Services v2 SDK 21.0.0 to 22.0.0. The 22.0.0 public API
  is additive over 21.0.0 (it adds `GameStatsClient` and `PlayerGameEvent`),
  every symbol this plugin calls is unchanged, and no plugin source changed.
- Android: Android Gradle Plugin 9.3.1, Kotlin Gradle Plugin 2.4.10.
- Toolchain to latest: TypeScript 6 to 7.0.2 (the native `tsgo` compiler),
  Biome 2.5.6, Capacitor 8.4.2, rollup 4.62.3, rimraf 6.1.3,
  `@capacitor/docgen` 0.3.1, pnpm 11.18.0.
- iOS: `capacitor-swift-pm` floor 8.4.2.
- CI: `actions/setup-node` v7.

### Requirements

- Android `minSdkVersion` 24. Play Games Services v2 SDK 22.0.0 raises its own
  floor from 21 to 24, so an app that sets `minSdkVersion` below 24 fails the
  manifest merge. This module already defaulted to 24, and Capacitor 8 requires
  24, so an app on stock Capacitor 8 settings needs no change.

## [0.2.0] - 2026-07-12

### Added

- `requestServerSideAccess({ serverClientId, forceRefresh? })` (Android): a
  one-time OAuth 2.0 server auth code for the signed-in Play Games player, for a
  backend to exchange for the authoritative player id (Play Games Services v2
  `GamesSignInClient.requestServerSideAccess`).
- `fetchIdentityVerificationSignature()` (iOS): a GameKit identity-verification
  bundle (`GKLocalPlayer.fetchItems(forIdentityVerificationSignature:)`) a backend
  verifies against Apple's certificate to trust the Game Center player id.
- Both methods have a safe no-op web fallback and reject as unimplemented on the
  non-owning native platform.

## [0.1.0] - 2026-06-20

### Added

- Initial release.
- Sign-in (silent + interactive) and player profile.
- Achievement unlock / increment and the native achievements UI.
- Leaderboard score submission and the native leaderboard UI.
- Saved Games (load / save / list / delete) with most-recently-modified conflict
  resolution.
- `signInStateChanged` event for system-driven auth changes.
- Android via Google Play Games Services v2, iOS via Apple GameKit, with a safe
  no-op web fallback. iOS registers via `CAPBridgedPlugin` (Capacitor 8 Swift
  registration, no Objective-C `.m` file required).
