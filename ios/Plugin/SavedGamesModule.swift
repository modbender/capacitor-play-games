import Capacitor
import GameKit

/// GameKit Saved Games: load/save/list/delete by stable name. Conflicts resolve
/// most-recently-modified-wins. Requires Game Center auth; otherwise GKLocalPlayer
/// fails with `GKErrorNotAuthenticated`, forwarded to the caller.
internal class SavedGamesModule: PgsModule {
    private var player: GKLocalPlayer { GKLocalPlayer.local }

    func load(_ call: CAPPluginCall) {
        guard let name = call.getString("name") else {
            call.reject("missing name"); return
        }
        player.fetchSavedGames { saved, error in
            if let error = error {
                call.reject(error.localizedDescription, nil, error)
                return
            }
            let matches = (saved ?? []).filter { $0.name == name }
            guard let pick = matches.max(by: { ($0.modificationDate ?? .distantPast) < ($1.modificationDate ?? .distantPast) }) else {
                call.resolve(["snapshot": NSNull()])
                return
            }
            if matches.count > 1 {
                self.player.resolveConflictingSavedGames(matches, with: Data()) { _, _ in }
            }
            pick.loadData { data, dataError in
                if let dataError = dataError {
                    call.reject(dataError.localizedDescription, nil, dataError)
                    return
                }
                let bytes = data ?? Data()
                let payload: [String: Any] = [
                    "name": pick.name ?? name,
                    // GKSavedGame has no description field; empty for API parity.
                    "description": "",
                    "modifiedAt": Int((pick.modificationDate ?? Date()).timeIntervalSince1970 * 1000),
                    "data": String(data: bytes, encoding: .utf8) ?? "",
                ]
                call.resolve(["snapshot": payload])
            }
        }
    }

    func save(_ call: CAPPluginCall) {
        guard let name = call.getString("name") else {
            call.reject("missing name"); return
        }
        guard let data = call.getString("data") else {
            call.reject("missing data"); return
        }
        let bytes = Data(data.utf8)
        player.saveGameData(bytes, withName: name) { _, error in
            if let error = error {
                call.reject(error.localizedDescription, nil, error)
            } else {
                call.resolve()
            }
        }
    }

    func list(_ call: CAPPluginCall) {
        player.fetchSavedGames { saved, error in
            if let error = error {
                call.reject(error.localizedDescription, nil, error)
                return
            }
            // Collapse cross-device conflicts to one entry per name.
            var byName: [String: GKSavedGame] = [:]
            for s in saved ?? [] {
                let key = s.name ?? ""
                if key.isEmpty { continue }
                if let existing = byName[key],
                   (existing.modificationDate ?? .distantPast) >= (s.modificationDate ?? .distantPast) {
                    continue
                }
                byName[key] = s
            }
            let snapshots = byName.values.map { s -> [String: Any] in
                [
                    "name": s.name ?? "",
                    "description": "",
                    "modifiedAt": Int((s.modificationDate ?? Date()).timeIntervalSince1970 * 1000),
                ]
            }
            call.resolve(["snapshots": snapshots])
        }
    }

    func delete(_ call: CAPPluginCall) {
        guard let name = call.getString("name") else {
            call.reject("missing name"); return
        }
        player.deleteSavedGames(withName: name) { error in
            if let error = error {
                call.reject(error.localizedDescription, nil, error)
            } else {
                call.resolve()
            }
        }
    }
}
