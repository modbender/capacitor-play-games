import Capacitor
import GameKit
import UIKit

/// GameKit has no increment API (report overwrites percentComplete), so
/// increment() loads current progress, adds the steps (percentage points),
/// then reports the new total.
internal class AchievementsModule: PgsModule, GKGameCenterControllerDelegate {
    func unlock(_ call: CAPPluginCall) {
        guard let id = call.getString("id") else {
            call.reject("missing id"); return
        }
        let achievement = GKAchievement(identifier: id)
        achievement.percentComplete = 100
        achievement.showsCompletionBanner = true
        GKAchievement.report([achievement]) { error in
            if let error = error {
                call.reject(error.localizedDescription, nil, error)
            } else {
                call.resolve()
            }
        }
    }

    func increment(_ call: CAPPluginCall) {
        guard let id = call.getString("id") else {
            call.reject("missing id"); return
        }
        let steps = call.getDouble("steps") ?? 0

        GKAchievement.loadAchievements { existing, error in
            if let error = error {
                call.reject(error.localizedDescription, nil, error)
                return
            }
            let current = existing?.first(where: { $0.identifier == id })
            let achievement = current ?? GKAchievement(identifier: id)
            let next = min(100, (achievement.percentComplete) + steps)
            achievement.percentComplete = next
            achievement.showsCompletionBanner = next >= 100
            GKAchievement.report([achievement]) { reportError in
                if let reportError = reportError {
                    call.reject(reportError.localizedDescription, nil, reportError)
                } else {
                    call.resolve()
                }
            }
        }
    }

    func show(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            guard let presenter = self.topViewController() else {
                call.reject("no presenter available"); return
            }
            let vc: GKGameCenterViewController
            if #available(iOS 14.0, *) {
                vc = GKGameCenterViewController(state: .achievements)
            } else {
                vc = GKGameCenterViewController()
                vc.viewState = .achievements
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
