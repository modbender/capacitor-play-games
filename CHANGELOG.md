# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
