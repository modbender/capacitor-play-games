// swift-tools-version: 5.9
import PackageDescription

// Package/product/target names carry the npm scope ("Idleflowgames…") because
// Capacitor's `cap sync ios` derives the SwiftPM product name from the FULL
// scoped package name (@idleflowgames/capacitor-play-games →
// IdleflowgamesCapacitorPlayGames, PascalCased scope+name, same as
// @capacitor-community/admob → CapacitorCommunityAdmob). The consuming app's
// generated Package.swift references that derived name, so the product here must
// match it exactly or `xcodebuild -resolvePackageDependencies` fails with
// "product 'IdleflowgamesCapacitorPlayGames' not found". The Swift module is
// renamed with it; nothing imports it by name and Capacitor registers the
// plugin via the Objective-C runtime, so the rename is source-compatible.
let package = Package(
    name: "IdleflowgamesCapacitorPlayGames",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "IdleflowgamesCapacitorPlayGames",
            targets: ["IdleflowgamesCapacitorPlayGames"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.4.2")
    ],
    targets: [
        .target(
            name: "IdleflowgamesCapacitorPlayGames",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Plugin"
        )
    ]
)
