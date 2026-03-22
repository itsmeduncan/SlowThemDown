import Foundation
import Testing
@testable import SlowThemDown

@Suite("LogViewModel")
@MainActor
struct LogViewModelTests {

    // MARK: - Filtering by search text

    @Test func filteredEntries_emptySearch_returnsAll() {
        let vm = LogViewModel()
        let entries = [
            SpeedEntry(speed: 8.94, streetName: "Main St"),
            SpeedEntry(speed: 13.41, streetName: "Oak Ave"),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 2)
    }

    @Test func filteredEntries_searchByStreetName() {
        let vm = LogViewModel()
        vm.searchText = "main"
        let entries = [
            SpeedEntry(speed: 8.94, streetName: "Main St"),
            SpeedEntry(speed: 13.41, streetName: "Oak Ave"),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 1)
        #expect(result[0].streetName == "Main St")
    }

    @Test func filteredEntries_searchByNotes() {
        let vm = LogViewModel()
        vm.searchText = "red sedan"
        let entries = [
            SpeedEntry(speed: 8.94, notes: "Red sedan speeding"),
            SpeedEntry(speed: 13.41, notes: "Blue truck"),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 1)
        #expect(result[0].notes == "Red sedan speeding")
    }

    @Test func filteredEntries_searchIsCaseInsensitive() {
        let vm = LogViewModel()
        vm.searchText = "MAIN"
        let entries = [SpeedEntry(speed: 8.94, streetName: "main street")]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 1)
    }

    // MARK: - Filtering by vehicle type

    @Test func filteredEntries_filterByVehicleType() {
        let vm = LogViewModel()
        vm.filterVehicleType = .truck
        let entries = [
            SpeedEntry(speed: 8.94, vehicleType: .car),
            SpeedEntry(speed: 13.41, vehicleType: .truck),
            SpeedEntry(speed: 17.88, vehicleType: .truck),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 2)
        #expect(result.allSatisfy { $0.vehicleType == .truck })
    }

    @Test func filteredEntries_nilVehicleFilter_returnsAll() {
        let vm = LogViewModel()
        vm.filterVehicleType = nil
        let entries = [
            SpeedEntry(speed: 8.94, vehicleType: .car),
            SpeedEntry(speed: 13.41, vehicleType: .truck),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 2)
    }

    // MARK: - Filtering by over limit

    @Test func filteredEntries_overLimitOnly() {
        let vm = LogViewModel()
        vm.filterOverLimit = true
        let limit = 11.176
        let entries = [
            SpeedEntry(speed: 8.94, speedLimit: limit),
            SpeedEntry(speed: 13.41, speedLimit: limit),
            SpeedEntry(speed: 17.88, speedLimit: limit),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 2)
        #expect(result.allSatisfy { $0.isOverLimit })
    }

    @Test func filteredEntries_overLimitFalse_returnsAll() {
        let vm = LogViewModel()
        vm.filterOverLimit = false
        let limit = 11.176
        let entries = [
            SpeedEntry(speed: 8.94, speedLimit: limit),
            SpeedEntry(speed: 13.41, speedLimit: limit),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 2)
    }

    // MARK: - Sorting

    @Test func filteredEntries_sortNewestFirst() {
        let vm = LogViewModel()
        vm.sortNewestFirst = true
        let older = SpeedEntry(speed: 8.94, timestamp: Date(timeIntervalSince1970: 1000))
        let newer = SpeedEntry(speed: 13.41, timestamp: Date(timeIntervalSince1970: 2000))
        let result = vm.filteredEntries([older, newer])
        #expect(result[0].timestamp > result[1].timestamp)
    }

    @Test func filteredEntries_sortOldestFirst() {
        let vm = LogViewModel()
        vm.sortNewestFirst = false
        let older = SpeedEntry(speed: 8.94, timestamp: Date(timeIntervalSince1970: 1000))
        let newer = SpeedEntry(speed: 13.41, timestamp: Date(timeIntervalSince1970: 2000))
        let result = vm.filteredEntries([older, newer])
        #expect(result[0].timestamp < result[1].timestamp)
    }

    // MARK: - Combined filters

    @Test func filteredEntries_combinedFilters() {
        let vm = LogViewModel()
        vm.searchText = "main"
        vm.filterVehicleType = .car
        vm.filterOverLimit = true
        let limit = 11.176

        let entries = [
            SpeedEntry(speed: 13.41, speedLimit: limit, streetName: "Main St", vehicleType: .car),
            SpeedEntry(speed: 13.41, speedLimit: limit, streetName: "Main St", vehicleType: .truck),
            SpeedEntry(speed: 8.94, speedLimit: limit, streetName: "Main St", vehicleType: .car),
            SpeedEntry(speed: 13.41, speedLimit: limit, streetName: "Oak Ave", vehicleType: .car),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 1)
        #expect(result[0].streetName == "Main St")
        #expect(result[0].vehicleType == .car)
        #expect(result[0].isOverLimit)
    }

    // MARK: - Empty input

    @Test func filteredEntries_emptyArray_returnsEmpty() {
        let vm = LogViewModel()
        let result = vm.filteredEntries([])
        #expect(result.isEmpty)
    }
}
