import Foundation
import Testing
@testable import SlowThemDown

@Suite("ReportViewModel")
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
        let entries = [
            SpeedEntry(speedMPH: 20, speedLimit: 25),
            SpeedEntry(speedMPH: 30, speedLimit: 25),
            SpeedEntry(speedMPH: 40, speedLimit: 25),
        ]
        vm.update(with: entries)
        #expect(vm.stats != nil)
        #expect(vm.stats!.count == 3)
        #expect(abs(vm.stats!.mean - 30) < 0.001)
    }

    // MARK: - Histogram

    @Test func histogram_bucketsAreCorrect() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speedMPH: 22),
            SpeedEntry(speedMPH: 23),
            SpeedEntry(speedMPH: 28),
            SpeedEntry(speedMPH: 33),
        ]
        vm.update(with: entries)

        #expect(!vm.histogram.isEmpty)
        let totalCount = vm.histogram.reduce(0) { $0 + $1.count }
        #expect(totalCount == 4)
    }

    @Test func histogram_singleSpeed_oneBucket() {
        let vm = ReportViewModel()
        let entries = [SpeedEntry(speedMPH: 27)]
        vm.update(with: entries)
        #expect(vm.histogram.count == 1)
        #expect(vm.histogram[0].count == 1)
    }

    @Test func histogram_bucketWidth_isFive() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speedMPH: 10),
            SpeedEntry(speedMPH: 20),
        ]
        vm.update(with: entries)
        // Buckets: 10-15, 15-20, 20-25
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
            SpeedEntry(speedMPH: 20, timestamp: date9am),
            SpeedEntry(speedMPH: 30, timestamp: date9am.addingTimeInterval(60)),
            SpeedEntry(speedMPH: 40, timestamp: date3pm),
        ]
        vm.update(with: entries)

        #expect(vm.hourlyAverages.count == 2)

        let hour9 = vm.hourlyAverages.first { $0.hour == 9 }
        #expect(hour9 != nil)
        #expect(abs(hour9!.averageSpeed - 25.0) < 0.001)

        let hour15 = vm.hourlyAverages.first { $0.hour == 15 }
        #expect(hour15 != nil)
        #expect(abs(hour15!.averageSpeed - 40.0) < 0.001)
    }

    @Test func hourlyAverages_sortedByHour() {
        let vm = ReportViewModel()
        let cal = Calendar.current
        let dateLate = cal.date(from: DateComponents(year: 2026, month: 3, day: 20, hour: 20))!
        let dateEarly = cal.date(from: DateComponents(year: 2026, month: 3, day: 20, hour: 8))!

        let entries = [
            SpeedEntry(speedMPH: 30, timestamp: dateLate),
            SpeedEntry(speedMPH: 25, timestamp: dateEarly),
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
            SpeedEntry(speedMPH: 30, timestamp: newer),
            SpeedEntry(speedMPH: 25, timestamp: older),
        ]
        vm.update(with: entries)
        #expect(vm.dailyEntries.count == 2)
        #expect(vm.dailyEntries[0].date < vm.dailyEntries[1].date)
    }

    @Test func dailyEntries_matchesInputCount() {
        let vm = ReportViewModel()
        let entries = (1...5).map { SpeedEntry(speedMPH: Double($0) * 10) }
        vm.update(with: entries)
        #expect(vm.dailyEntries.count == 5)
    }

    // MARK: - Street Filtering

    @Test func availableStreets_excludesEmpty() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speedMPH: 30, streetName: "Oak St"),
            SpeedEntry(speedMPH: 25, streetName: ""),
            SpeedEntry(speedMPH: 35, streetName: "Elm Ave"),
        ]
        vm.update(with: entries)
        #expect(vm.availableStreets.count == 2)
        #expect(vm.availableStreets.contains("Oak St"))
        #expect(vm.availableStreets.contains("Elm Ave"))
    }

    @Test func availableStreets_sorted() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speedMPH: 30, streetName: "Zephyr Rd"),
            SpeedEntry(speedMPH: 25, streetName: "Ash Ln"),
        ]
        vm.update(with: entries)
        #expect(vm.availableStreets == ["Ash Ln", "Zephyr Rd"])
    }

    @Test func availableStreets_deduplicates() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speedMPH: 30, streetName: "Oak St"),
            SpeedEntry(speedMPH: 25, streetName: "Oak St"),
            SpeedEntry(speedMPH: 35, streetName: "Elm Ave"),
        ]
        vm.update(with: entries)
        #expect(vm.availableStreets.count == 2)
    }

    @Test func selectStreet_filtersStats() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speedMPH: 20, speedLimit: 25, streetName: "Oak St"),
            SpeedEntry(speedMPH: 30, speedLimit: 25, streetName: "Oak St"),
            SpeedEntry(speedMPH: 50, speedLimit: 25, streetName: "Elm Ave"),
        ]
        vm.update(with: entries)

        vm.selectStreet("Oak St")
        #expect(vm.stats!.count == 2)
        #expect(abs(vm.stats!.mean - 25.0) < 0.001)
    }

    @Test func selectStreet_nil_showsAll() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speedMPH: 20, streetName: "Oak St"),
            SpeedEntry(speedMPH: 30, streetName: "Elm Ave"),
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
            SpeedEntry(speedMPH: 20, streetName: "Oak St"),
            SpeedEntry(speedMPH: 30, streetName: "Elm Ave"),
            SpeedEntry(speedMPH: 40, streetName: "Oak St"),
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
            SpeedEntry(speedMPH: 20, streetName: "Oak St"),
            SpeedEntry(speedMPH: 30, streetName: "Oak St"),
            SpeedEntry(speedMPH: 40, streetName: "Oak St"),
            SpeedEntry(speedMPH: 25, streetName: "Elm Ave"),
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
        let entries = [
            SpeedEntry(speedMPH: 20, speedLimit: 25, streetName: "Oak St"),
            SpeedEntry(speedMPH: 30, speedLimit: 25, streetName: "Oak St"),
        ]
        vm.update(with: entries)
        #expect(vm.streetGroups.count == 1)
        #expect(abs(vm.streetGroups[0].meanSpeed - 25.0) < 0.001)
        #expect(abs(vm.streetGroups[0].overLimitPercent - 50.0) < 0.001)
    }

    @Test func streetGroups_excludesEmptyStreetNames() {
        let vm = ReportViewModel()
        let entries = [
            SpeedEntry(speedMPH: 20, streetName: ""),
            SpeedEntry(speedMPH: 30, streetName: "Oak St"),
        ]
        vm.update(with: entries)
        #expect(vm.streetGroups.count == 1)
        #expect(vm.streetGroups[0].name == "Oak St")
    }
}
