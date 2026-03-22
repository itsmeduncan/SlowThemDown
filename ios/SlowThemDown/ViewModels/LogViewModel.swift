import Foundation
import SwiftData

@MainActor
@Observable
final class LogViewModel {
    var searchText: String = ""
    var filterVehicleType: VehicleType?
    var filterOverLimit: Bool = false
    var sortNewestFirst: Bool = true

    func filteredEntries(_ entries: [SpeedEntry]) -> [SpeedEntry] {
        var result = entries

        if !searchText.isEmpty {
            let query = searchText.lowercased()
            result = result.filter {
                $0.streetName.lowercased().contains(query) ||
                $0.notes.lowercased().contains(query)
            }
        }

        if let vehicleFilter = filterVehicleType {
            result = result.filter { $0.vehicleType == vehicleFilter }
        }

        if filterOverLimit {
            result = result.filter(\.isOverLimit)
        }

        result.sort { a, b in
            sortNewestFirst ? a.timestamp > b.timestamp : a.timestamp < b.timestamp
        }

        return result
    }
}
