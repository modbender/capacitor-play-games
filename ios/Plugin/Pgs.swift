import Capacitor
import Foundation
import GameKit
import UIKit

/// Shared base for per-feature modules. Inherits `NSObject` so subclasses can
/// adopt @objc protocols (e.g. `GKGameCenterControllerDelegate`).
internal class PgsModule: NSObject {
    unowned let plugin: PlayGamesPlugin
    init(_ plugin: PlayGamesPlugin) {
        self.plugin = plugin
        super.init()
    }

    /// Top-most presented view controller. Callers must be on the main queue.
    func topViewController() -> UIViewController? {
        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive })
            ?? UIApplication.shared.connectedScenes
                .compactMap({ $0 as? UIWindowScene }).first,
            let rootVC = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController
                ?? scene.windows.first?.rootViewController
        else { return nil }

        var top = rootVC
        while let presented = top.presentedViewController { top = presented }
        return top
    }
}

/// Serialise a GameKit player as the JS-side `PlayerInfo`. Uses the stable
/// cross-device `gamePlayerID` (not the per-team `teamPlayerID`).
internal func playerInfo(from player: GKLocalPlayer) -> [String: Any] {
    let info: [String: Any] = [
        "playerId": player.gamePlayerID,
        "displayName": player.displayName,
    ]
    return info
}
