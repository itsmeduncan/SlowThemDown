import Foundation
import Testing
@testable import SlowThemDown

@Suite("ReportViewModel")
@MainActor
struct ReportViewModelTests {

    // MARK: - update(with:)

    @Test func update_withEmptyEntries_clearsState() {
        let vm = ReportViewModel()
        vm.update(with: [])
        #expect(vm.entries.isEmpty)
        #expect(vm.stats == nil)
        #expect(vm.histogram.isEmpty)
        #expect(vm.hourlyAverages.isEmpty)
        #expect(vm.dailyEntries.isEmpty)
    }

    @Test func update_computesStats() {
        let vm = ReportViewModel()
        let limit = 11.176
        let entries = [
            SpeedEntry(speed: 8.94, speedLimit: limit),
            SpeedEntry(speed: 13.41, speedLimit: limit),
            SpeedEntry(speed: 17.88, speedLimit: limit),
        ]
        vm.update(with: entries)
        #expect(vm.stats != nil)
        #expect(vm.stats!.count == 3)
        #expect(abs(vm.stats!.mean - 13.41) < 0.01)
    }

    // MARK: - Histogram

    @Test func histogram_bucketsAreCorrect() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speed: 9.83),
            SpeedEntry(speed: 10.29),
            SpeedEntry(speed: 12.52),
            SpeedEntry(speed: 14.76),
        ]
        vm.update(with: entries)

        #expect(!vm.histogram.isEmpty)
        let totalCount = vm.histogram.reduce(0) { $0 + $1.count }
        #expect(totalCount == 4)
    }

    @Test func histogram_singleSpeed_oneBucket() {
        let vm = ReportViewModel()
        let entries = [SpeedEntry(speed: 12.07)]
        vm.update(with: entries)
        // The single speed should be in exactly one bucket
        let totalCount = vm.histogram.reduce(0) { $0 + $1.count }
        #expect(totalCount == 1)
        #expect(!vm.histogram.isEmpty)
    }

    @Test func histogram_multipleSpeeds_hasBuckets() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speed: 4.47),
            SpeedEntry(speed: 8.94),
        ]
        vm.update(with: entries)
        #expect(vm.histogram.count >= 2)
        for bucket in vm.histogram {
            #expect(bucket.range.contains("-"))
        }
    }

    // MARK: - Hourly Averages

    @Test func hourlyAverages_groupsByHour() {
        let vm = ReportViewModel()
        let cal = Calendar.current
        let date9am = cal.date(from: DateComponents(year: 2026, month: 3, day: 20, hour: 9))!
        let date3pm = cal.date(from: DateComponents(year: 2026, month: 3, day: 20, hour: 15))!

        let entries = [
            SpeedEntry(speed: 8.94, timestamp: date9am),
            SpeedEntry(speed: 13.41, timestamp: date9am.addingTimeInterval(60)),
            SpeedEntry(speed: 17.88, timestamp: date3pm),
        ]
        vm.update(with: entries)

        #expect(vm.hourlyAverages.count == 2)

        let hour9 = vm.hourlyAverages.first { $0.hour == 9 }
        #expect(hour9 != nil)
        #expect(abs(hour9!.averageSpeed - 11.175) < 0.01)

        let hour15 = vm.hourlyAverages.first { $0.hour == 15 }
        #expect(hour15 != nil)
        #expect(abs(hour15!.averageSpeed - 17.88) < 0.001)
    }

    @Test func hourlyAverages_sortedByHour() {
        let vm = ReportViewModel()
        let cal = Calendar.current
        let dateLate = cal.date(from: DateComponents(year: 2026, month: 3, day: 20, hour: 20))!
        let dateEarly = cal.date(from: DateComponents(year: 2026, month: 3, day: 20, hour: 8))!

        let entries = [
            SpeedEntry(speed: 13.41, timestamp: dateLate),
            SpeedEntry(speed: 11.18, timestamp: dateEarly),
        ]
        vm.update(with: entries)
        #expect(vm.hourlyAverages[0].hour < vm.hourlyAverages[1].hour)
    }

    // MARK: - Daily Entries

    @Test func dailyEntries_sortedByDate() {
        let vm = ReportViewModel()
        let older = Date(timeIntervalSince1970: 1000)
        let newer = Date(timeIntervalSince1970: 2000)

        let entries = [
            SpeedEntry(speed: 13.41, timestamp: newer),
            SpeedEntry(speed: 11.18, timestamp: older),
        ]
        vm.update(with: entries)
        #expect(vm.dailyEntries.count == 2)
        #expect(vm.dailyEntries[0].date < vm.dailyEntries[1].date)
    }

    @Test func dailyEntries_matchesInputCount() {
        let vm = ReportViewModel()
        let entries = (1...5).map { SpeedEntry(speed: Double($0) * 4.47) }
        vm.update(with: entries)
        #expect(vm.dailyEntries.count == 5)
    }

    // MARK: - Street Filtering

    @Test func availableStreets_excludesEmpty() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speed: 13.41, streetName: "Oak St"),
            SpeedEntry(speed: 11.18, streetName: ""),
            SpeedEntry(speed: 15.65, streetName: "Elm Ave"),
        ]
        vm.update(with: entries)
        #expect(vm.availableStreets.count == 2)
        #expect(vm.availableStreets.contains("Oak St"))
        #expect(vm.availableStreets.contains("Elm Ave"))
    }

    @Test func availableStreets_sorted() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speed: 13.41, streetName: "Zephyr Rd"),
            SpeedEntry(speed: 11.18, streetName: "Ash Ln"),
        ]
        vm.update(with: entries)
        #expect(vm.availableStreets == ["Ash Ln", "Zephyr Rd"])
    }

    @Test func availableStreets_deduplicates() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speed: 13.41, streetName: "Oak St"),
            SpeedEntry(speed: 11.18, streetName: "Oak St"),
            SpeedEntry(speed: 15.65, streetName: "Elm Ave"),
        ]
        vm.update(with: entries)
        #expect(vm.availableStreets.count == 2)
    }

    @Test func selectStreet_filtersStats() {
        let vm = ReportViewModel()
        let limit = 11.176
        let entries = [
            SpeedEntry(speed: 8.94, speedLimit: limit, streetName: "Oak St"),
            SpeedEntry(speed: 13.41, speedLimit: limit, streetName: "Oak St"),
            SpeedEntry(speed: 22.35, speedLimit: limit, streetName: "Elm Ave"),
        ]
        vm.update(with: entries)

        vm.selectStreet("Oak St")
        #expect(vm.stats!.count == 2)
        #expect(abs(vm.stats!.mean - 11.175) < 0.01)
    }

    @Test func selectStreet_nil_showsAll() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speed: 8.94, streetName: "Oak St"),
            SpeedEntry(speed: 13.41, streetName: "Elm Ave"),
        ]
        vm.update(with: entries)

        vm.selectStreet("Oak St")
        #expect(vm.stats!.count == 1)

        vm.selectStreet(nil)
        #expect(vm.stats!.count == 2)
    }

    @Test func filteredEntries_matchesSelectedStreet() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speed: 8.94, streetName: "Oak St"),
            SpeedEntry(speed: 13.41, streetName: "Elm Ave"),
            SpeedEntry(speed: 17.88, streetName: "Oak St"),
        ]
        vm.update(with: entries)

        vm.selectStreet("Oak St")
        #expect(vm.filteredEntries.count == 2)
        #expect(vm.filteredEntries.allSatisfy { $0.streetName == "Oak St" })
    }

    // MARK: - Street Groups

    @Test func streetGroups_sortedByCount() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speed: 8.94, streetName: "Oak St"),
            SpeedEntry(speed: 13.41, streetName: "Oak St"),
            SpeedEntry(speed: 17.88, streetName: "Oak St"),
            SpeedEntry(speed: 11.18, streetName: "Elm Ave"),
        ]
        vm.update(with: entries)
        #expect(vm.streetGroups.count == 2)
        #expect(vm.streetGroups[0].name == "Oak St")
        #expect(vm.streetGroups[0].count == 3)
        #expect(vm.streetGroups[1].name == "Elm Ave")
        #expect(vm.streetGroups[1].count == 1)
    }

    @Test func streetGroups_computesMeanAndOverLimit() {
        let vm = ReportViewModel()
        let limit = 11.176
        let entries = [
            SpeedEntry(speed: 8.94, speedLimit: limit, streetName: "Oak St"),
            SpeedEntry(speed: 13.41, speedLimit: limit, streetName: "Oak St"),
        ]
        vm.update(with: entries)
        #expect(vm.streetGroups.count == 1)
        #expect(abs(vm.streetGroups[0].meanSpeed - 11.175) < 0.01)
        #expect(abs(vm.streetGroups[0].overLimitPercent - 50.0) < 0.001)
    }

    @Test func streetGroups_excludesEmptyStreetNames() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speed: 8.94, streetName: ""),
            SpeedEntry(speed: 13.41, streetName: "Oak St"),
        ]
        vm.update(with: entries)
        #expect(vm.streetGroups.count == 1)
        #expect(vm.streetGroups[0].name == "Oak St")
    }
}
