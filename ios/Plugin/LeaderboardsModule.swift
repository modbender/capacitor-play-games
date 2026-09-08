import Capacitor
import GameKit
import UIKit

/// GameKit leaderboards: score submission + the native leaderboard overlay.
internal class LeaderboardsModule: PgsModule, GKGameCenterControllerDelegate {
    func submit(_ call: CAPPluginCall) {
        guard let leaderboardID = call.getString("leaderboardId") else {
            call.reject("missing leaderboardId"); return
        }
        let scoreDouble = call.getDouble("score") ?? 0
        let score = Int(scoreDouble)

        GKLeaderboard.submitScore(
            score,
            context: 0,
            player: GKLocalPlayer.local,
            leaderboardIDs: [leaderboardID]
        ) { error in
            if let error = error {
                call.reject(error.localizedDescription, nil, error)
            } else {
                call.resolve()
            }
        }
    }

    func show(_ call: CAPPluginCall) {
        guard let leaderboardID = call.getString("leaderboardId") else {
            call.reject("missing leaderboardId"); return
        }
        DispatchQueue.main.async {
            guard let presenter = self.topViewController() else {
                call.reject("no presenter available"); return
            }
            let vc: GKGameCenterViewController
            if #available(iOS 14.0, *) {
                vc = GKGameCenterViewController(
                    leaderboardID: leaderboardID,
                    playerScope: .global,
                    timeScope: .allTime
                )
            } else {
                vc = GKGameCenterViewController()
                vc.viewState = .leaderboards
                vc.leaderboardIdentifier = leaderboardID
            }
            vc.gameCenterDelegate = self
            presenter.present(vc, animated: true) { call.resolve() }
        }
    }

    func showAll(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            guard let presenter = self.topViewController() else {
                call.reject("no presenter available"); return
            }
            let vc: GKGameCenterViewController
            if #available(iOS 14.0, *) {
                vc = GKGameCenterViewController(state: .leaderboards)
            } else {
                vc = GKGameCenterViewController()
                vc.viewState = .leaderboards
            }
            vc.gameCenterDelegate = self
            presenter.present(vc, animated: true) { call.resolve() }
        }
    }

    // MARK: - GKGameCenterControllerDelegate

    func gameCenterViewControllerDidFinish(_ controller: GKGameCenterViewController) {
        controller.dismiss(animated: true)
    }
}
