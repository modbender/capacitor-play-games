// swift-tools-version: 5.9
import PackageDescription

// Package/product/target names carry the npm scope ("Modbender…") because
// Capacitor's `cap sync ios` derives the SwiftPM product name from the FULL
// scoped package name (@modbender/capacitor-play-games →
// ModbenderCapacitorPlayGames, PascalCased scope+name, same as
// @capacitor-community/admob → CapacitorCommunityAdmob). The consuming app's
// generated Package.swift references that derived name, so the product here must
// match it exactly or `xcodebuild -resolvePackageDependencies` fails with
// "product 'ModbenderCapacitorPlayGames' not found". The Swift module is
// renamed with it; nothing imports it by name and Capacitor registers the
// plugin via the Objective-C runtime, so the rename is source-compatible.
let package = Package(
    name: "ModbenderCapacitorPlayGames",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "ModbenderCapacitorPlayGames",
            targets: ["ModbenderCapacitorPlayGames"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.4.2")
    ],
    targets: [
        .target(
            name: "ModbenderCapacitorPlayGames",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Plugin"
        )
    ]
)
