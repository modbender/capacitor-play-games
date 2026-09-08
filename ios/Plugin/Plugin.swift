import Capacitor
import Foundation
import GameKit

/// Capacitor 8 plugin wrapping Apple GameKit (sign-in, achievements,
/// leaderboards, saved games). Thin dispatcher to four per-feature modules.
@objc(PlayGamesPlugin)
public class PlayGamesPlugin: CAPPlugin, CAPBridgedPlugin {
    // CAPBridgedPlugin self-registers the plugin on iOS (Capacitor 8). `jsName`
    // must match registerPlugin("PlayGames"); addListener/removeAllListeners
    // come from CAPPlugin and aren't listed here.
    public let identifier = "PlayGamesPlugin"
    public let jsName = "PlayGames"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "initialize", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "signIn", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isSignedIn", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPlayer", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "fetchIdentityVerificationSignature", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "unlockAchievement", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "incrementAchievement", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showAchievements", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "submitScore", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showLeaderboard", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showAllLeaderboards", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "loadSnapshot", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "saveSnapshot", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "listSnapshots", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "deleteSnapshot", returnType: CAPPluginReturnPromise),
    ]

    private lazy var signInModule = SignInModule(self)
    private lazy var achievementsModule = AchievementsModule(self)
    private lazy var leaderboardsModule = LeaderboardsModule(self)
    private lazy var savedGamesModule = SavedGamesModule(self)

    // The GameKit authenticateHandler is installed in initialize(), NOT load():
    // see initialize() for the ATT-ordering reason.

    /// Expose listener emission to the modules.
    internal func emit(event: String, data: [String: Any]) {
        notifyListeners(event, data: data)
    }

    // MARK: - JS-callable surface

    @objc func initialize(_ call: CAPPluginCall) {
        // Setting the handler can auto-present GameKit's "Welcome back" banner,
        // which suppresses an ATT prompt shown while it's on screen, so call
        // initialize() only after the app has requested ATT.
        signInModule.installAuthHandlerIfNeeded()
        call.resolve()
    }

    @objc func signIn(_ call: CAPPluginCall) { signInModule.signIn(call) }
    @objc func isSignedIn(_ call: CAPPluginCall) { signInModule.isSignedIn(call) }
    @objc func getPlayer(_ call: CAPPluginCall) { signInModule.getPlayer(call) }
    @objc func fetchIdentityVerificationSignature(_ call: CAPPluginCall) {
        signInModule.fetchIdentityVerificationSignature(call)
    }

    @objc func unlockAchievement(_ call: CAPPluginCall) { achievementsModule.unlock(call) }
    @objc func incrementAchievement(_ call: CAPPluginCall) { achievementsModule.increment(call) }
    @objc func showAchievements(_ call: CAPPluginCall) { achievementsModule.show(call) }

    @objc func submitScore(_ call: CAPPluginCall) { leaderboardsModule.submit(call) }
    @objc func showLeaderboard(_ call: CAPPluginCall) { leaderboardsModule.show(call) }
    @objc func showAllLeaderboards(_ call: CAPPluginCall) { leaderboardsModule.showAll(call) }

    @objc func loadSnapshot(_ call: CAPPluginCall) { savedGamesModule.load(call) }
    @objc func saveSnapshot(_ call: CAPPluginCall) { savedGamesModule.save(call) }
    @objc func listSnapshots(_ call: CAPPluginCall) { savedGamesModule.list(call) }
    @objc func deleteSnapshot(_ call: CAPPluginCall) { savedGamesModule.delete(call) }
}
