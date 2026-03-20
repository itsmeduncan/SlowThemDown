import Foundation
import Testing
@testable import SlowThemDown

@Suite("LogViewModel")
struct LogViewModelTests {

    // MARK: - Filtering by search text

    @Test func filteredEntries_emptySearch_returnsAll() {
        let vm = LogViewModel()
        let entries = [
            SpeedEntry(speedMPH: 20, streetName: "Main St"),
            SpeedEntry(speedMPH: 30, streetName: "Oak Ave"),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 2)
    }

    @Test func filteredEntries_searchByStreetName() {
        let vm = LogViewModel()
        vm.searchText = "main"
        let entries = [
            SpeedEntry(speedMPH: 20, streetName: "Main St"),
            SpeedEntry(speedMPH: 30, streetName: "Oak Ave"),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 1)
        #expect(result[0].streetName == "Main St")
    }

    @Test func filteredEntries_searchByNotes() {
        let vm = LogViewModel()
        vm.searchText = "red sedan"
        let entries = [
            SpeedEntry(speedMPH: 20, notes: "Red sedan speeding"),
            SpeedEntry(speedMPH: 30, notes: "Blue truck"),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 1)
        #expect(result[0].notes == "Red sedan speeding")
    }

    @Test func filteredEntries_searchIsCaseInsensitive() {
        let vm = LogViewModel()
        vm.searchText = "MAIN"
        let entries = [SpeedEntry(speedMPH: 20, streetName: "main street")]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 1)
    }

    // MARK: - Filtering by vehicle type

    @Test func filteredEntries_filterByVehicleType() {
        let vm = LogViewModel()
        vm.filterVehicleType = .truck
        let entries = [
            SpeedEntry(speedMPH: 20, vehicleType: .car),
            SpeedEntry(speedMPH: 30, vehicleType: .truck),
            SpeedEntry(speedMPH: 40, vehicleType: .truck),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 2)
        #expect(result.allSatisfy { $0.vehicleType == .truck })
    }

    @Test func filteredEntries_nilVehicleFilter_returnsAll() {
        let vm = LogViewModel()
        vm.filterVehicleType = nil
        let entries = [
            SpeedEntry(speedMPH: 20, vehicleType: .car),
            SpeedEntry(speedMPH: 30, vehicleType: .truck),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 2)
    }

    // MARK: - Filtering by over limit

    @Test func filteredEntries_overLimitOnly() {
        let vm = LogViewModel()
        vm.filterOverLimit = true
        let entries = [
            SpeedEntry(speedMPH: 20, speedLimit: 25),
            SpeedEntry(speedMPH: 30, speedLimit: 25),
            SpeedEntry(speedMPH: 40, speedLimit: 25),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 2)
        #expect(result.allSatisfy { $0.isOverLimit })
    }

    @Test func filteredEntries_overLimitFalse_returnsAll() {
        let vm = LogViewModel()
        vm.filterOverLimit = false
        let entries = [
            SpeedEntry(speedMPH: 20, speedLimit: 25),
            SpeedEntry(speedMPH: 30, speedLimit: 25),
        ]
        let result = vm.filteredEntries(entries)
        #expect(result.count == 2)
    }

    // MARK: - Sorting

    @Test func filteredEntries_sortNewestFirst() {
        let vm = LogViewModel()
        vm.sortNewestFirst = true
        let older = SpeedEntry(speedMPH: 20, timestamp: Date(timeIntervalSince1970: 1000))
        let newer = SpeedEntry(speedMPH: 30, timestamp: Date(timeIntervalSince1970: 2000))
        let result = vm.filteredEntries([older, newer])
        #expect(result[0].timestamp > result[1].timestamp)
    }

    @Test func filteredEntries_sortOldestFirst() {
        let vm = LogViewModel()
        vm.sortNewestFirst = false
        let older = SpeedEntry(speedMPH: 20, timestamp: Date(timeIntervalSince1970: 1000))
        let newer = SpeedEntry(speedMPH: 30, timestamp: Date(timeIntervalSince1970: 2000))
        let result = vm.filteredEntries([older, newer])
        #expect(result[0].timestamp < result[1].timestamp)
    }

    // MARK: - Combined filters

    @Test func filteredEntries_combinedFilters() {
        let vm = LogViewModel()
        vm.searchText = "main"
        vm.filterVehicleType = .car
        vm.filterOverLimit = true

        let entries = [
            SpeedEntry(speedMPH: 30, speedLimit: 25, streetName: "Main St", vehicleType: .car),
            SpeedEntry(speedMPH: 30, speedLimit: 25, streetName: "Main St", vehicleType: .truck),
            SpeedEntry(speedMPH: 20, speedLimit: 25, streetName: "Main St", vehicleType: .car),
            SpeedEntry(speedMPH: 30, speedLimit: 25, streetName: "Oak Ave", vehicleType: .car),
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
