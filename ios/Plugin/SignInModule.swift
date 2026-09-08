import Capacitor
import GameKit

/// Wraps `GKLocalPlayer.authenticateHandler` (callback-driven; fires on every
/// auth state change). `silent=true` only inspects `isAuthenticated`;
/// `silent=false` presents the system sign-in UI when GameKit offers it.
internal class SignInModule: PgsModule {
    private var handlerInstalled = false

    /// Idempotent. NOT installed in load(): setting the handler can auto-present
    /// GameKit's "Welcome back" banner, which would suppress an ATT prompt racing
    /// boot, so it is held until the app has requested ATT and called initialize().
    func installAuthHandlerIfNeeded() {
        if handlerInstalled { return }
        handlerInstalled = true

        GKLocalPlayer.local.authenticateHandler = { [weak self] viewController, _ in
            guard let self = self else { return }
            if let vc = viewController {
                // Stash the sign-in VC for the next interactive signIn(); don't auto-present.
                self.pendingSignInVC = vc
            } else {
                self.pendingSignInVC = nil
                // No UI offered: re-emit current auth state (also fires on sign-out).
                self.emitState()
            }
        }
    }

    private var pendingSignInVC: UIViewController?

    func signIn(_ call: CAPPluginCall) {
        let silent = call.getBool("silent", true)
        installAuthHandlerIfNeeded()

        if GKLocalPlayer.local.isAuthenticated {
            resolveSignedIn(call)
            return
        }

        if silent {
            resolveSignedOut(call)
            return
        }

        // Interactive: present the pending GameKit sign-in VC, if any.
        DispatchQueue.main.async {
            if let vc = self.pendingSignInVC, let presenter = self.topViewController() {
                self.pendingSignInVC = nil
                presenter.present(vc, animated: true) {
                    if GKLocalPlayer.local.isAuthenticated {
                        self.resolveSignedIn(call)
                    } else {
                        self.resolveSignedOut(call)
                    }
                }
            } else if GKLocalPlayer.local.isAuthenticated {
                self.resolveSignedIn(call)
            } else {
                // No sign-in UI to present (device signed out of Game Center, or
                // the app isn't recognised by Game Center yet).
                self.resolveSignedOut(call)
            }
        }
    }

    func isSignedIn(_ call: CAPPluginCall) {
        call.resolve(["signedIn": GKLocalPlayer.local.isAuthenticated])
    }

    func getPlayer(_ call: CAPPluginCall) {
        guard GKLocalPlayer.local.isAuthenticated else {
            call.reject("not signed in")
            return
        }
        call.resolve(playerInfo(from: GKLocalPlayer.local))
    }

    func fetchIdentityVerificationSignature(_ call: CAPPluginCall) {
        let localPlayer = GKLocalPlayer.local
        guard localPlayer.isAuthenticated else {
            call.reject("not signed in")
            return
        }
        localPlayer.fetchItems(forIdentityVerificationSignature: {
            publicKeyURL, signature, salt, timestamp, error in
            if let error = error {
                call.reject(error.localizedDescription, nil, error)
                return
            }
            guard let publicKeyURL = publicKeyURL,
                let signature = signature,
                let salt = salt
            else {
                call.reject("identity verification signature unavailable")
                return
            }
            call.resolve([
                "publicKeyUrl": publicKeyURL.absoluteString,
                "signature": signature.base64EncodedString(),
                "salt": salt.base64EncodedString(),
                "timestamp": timestamp,
                "playerId": localPlayer.gamePlayerID,
                "bundleId": Bundle.main.bundleIdentifier ?? "",
                "teamPlayerId": localPlayer.teamPlayerID,
                "gamePlayerId": localPlayer.gamePlayerID,
            ])
        })
    }

    private func resolveSignedIn(_ call: CAPPluginCall) {
        let body: [String: Any] = [
            "signedIn": true,
            "player": playerInfo(from: GKLocalPlayer.local),
        ]
        plugin.emit(event: "signInStateChanged", data: body)
        call.resolve(body)
    }

    private func resolveSignedOut(_ call: CAPPluginCall) {
        let body: [String: Any] = ["signedIn": false]
        plugin.emit(event: "signInStateChanged", data: body)
        call.resolve(body)
    }

    /// Re-emit current auth state on a handler-driven change (e.g. signed out via Settings).
    private func emitState() {
        let body: [String: Any]
        if GKLocalPlayer.local.isAuthenticated {
            body = [
                "signedIn": true,
                "player": playerInfo(from: GKLocalPlayer.local),
            ]
        } else {
            body = ["signedIn": false]
        }
        plugin.emit(event: "signInStateChanged", data: body)
    }
}
