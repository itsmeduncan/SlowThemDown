import Foundation

struct SpeedBucket: Identifiable {
    let id = UUID()
    let range: String
    let count: Int
    let lowerBound: Double
}

struct HourlyAverage: Identifiable {
    let id = UUID()
    let hour: Int
    let averageSpeed: Double

    var hourLabel: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "ha"
        var components = DateComponents()
        components.hour = hour
        let date = Calendar.current.date(from: components) ?? .now
        return formatter.string(from: date)
    }
}

struct DailyEntry: Identifiable {
    let id = UUID()
    let date: Date
    let speed: Double
}

struct StreetGroup: Identifiable {
    let id = UUID()
    let name: String
    let count: Int
    let meanSpeed: Double
    let overLimitPercent: Double
}

@Observable
final class ReportViewModel {
    var entries: [SpeedEntry] = []
    var stats: TrafficStats?
    var histogram: [SpeedBucket] = []
    var hourlyAverages: [HourlyAverage] = []
    var dailyEntries: [DailyEntry] = []

    var selectedStreet: String?
    var availableStreets: [String] = []
    var streetGroups: [StreetGroup] = []

    var filteredEntries: [SpeedEntry] {
        guard let street = selectedStreet else { return entries }
        return entries.filter { $0.streetName == street }
    }

    func update(with entries: [SpeedEntry]) {
        self.entries = entries
        buildAvailableStreets()
        buildStreetGroups()
        recompute()
    }

    func selectStreet(_ street: String?) {
        selectedStreet = street
        recompute()
    }

    private func recompute() {
        let active = filteredEntries
        stats = SpeedCalculator.trafficStats(entries: active)
        buildHistogram(from: active)
        buildHourlyAverages(from: active)
        buildDailyEntries(from: active)
    }

    private func buildAvailableStreets() {
        let streets = Set(entries.compactMap { $0.streetName.isEmpty ? nil : $0.streetName })
        availableStreets = streets.sorted()
    }

    private func buildStreetGroups() {
        let grouped = Dictionary(grouping: entries.filter { !$0.streetName.isEmpty }) { $0.streetName }
        streetGroups = grouped.map { street, streetEntries in
            let mean = streetEntries.map(\.speedMPH).reduce(0, +) / Double(streetEntries.count)
            let overCount = streetEntries.filter(\.isOverLimit).count
            let overPercent = Double(overCount) / Double(streetEntries.count) * 100
            return StreetGroup(name: street, count: streetEntries.count, meanSpeed: mean, overLimitPercent: overPercent)
        }.sorted { $0.count > $1.count }
    }

    private func buildHistogram(from entries: [SpeedEntry]) {
        let bucketSize = 5.0
        let speeds = entries.map(\.speedMPH)
        guard let minSpeed = speeds.min(), let maxSpeed = speeds.max() else {
            histogram = []
            return
        }

        let startBucket = floor(minSpeed / bucketSize) * bucketSize
        let endBucket = ceil(maxSpeed / bucketSize) * bucketSize
        var buckets: [SpeedBucket] = []

        var lower = startBucket
        while lower < endBucket {
            let upper = lower + bucketSize
            let count = speeds.filter { $0 >= lower && $0 < upper }.count
            buckets.append(SpeedBucket(
                range: "\(Int(lower))-\(Int(upper))",
                count: count,
                lowerBound: lower
            ))
            lower = upper
        }
        histogram = buckets
    }

    private func buildHourlyAverages(from entries: [SpeedEntry]) {
        let calendar = Calendar.current
        let grouped = Dictionary(grouping: entries) { entry in
            calendar.component(.hour, from: entry.timestamp)
        }
        hourlyAverages = grouped.map { hour, entries in
            let avg = entries.map(\.speedMPH).reduce(0, +) / Double(entries.count)
            return HourlyAverage(hour: hour, averageSpeed: avg)
        }.sorted { $0.hour < $1.hour }
    }

    private func buildDailyEntries(from entries: [SpeedEntry]) {
        dailyEntries = entries.map {
            DailyEntry(date: $0.timestamp, speed: $0.speedMPH)
        }.sorted { $0.date < $1.date }
    }
}
