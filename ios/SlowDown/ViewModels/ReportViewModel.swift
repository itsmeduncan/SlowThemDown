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

@Observable
final class ReportViewModel {
    var entries: [SpeedEntry] = []
    var stats: TrafficStats?
    var histogram: [SpeedBucket] = []
    var hourlyAverages: [HourlyAverage] = []
    var dailyEntries: [DailyEntry] = []

    func update(with entries: [SpeedEntry]) {
        self.entries = entries
        stats = SpeedCalculator.trafficStats(entries: entries)
        buildHistogram()
        buildHourlyAverages()
        buildDailyEntries()
    }

    private func buildHistogram() {
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

    private func buildHourlyAverages() {
        let calendar = Calendar.current
        let grouped = Dictionary(grouping: entries) { entry in
            calendar.component(.hour, from: entry.timestamp)
        }
        hourlyAverages = grouped.map { hour, entries in
            let avg = entries.map(\.speedMPH).reduce(0, +) / Double(entries.count)
            return HourlyAverage(hour: hour, averageSpeed: avg)
        }.sorted { $0.hour < $1.hour }
    }

    private func buildDailyEntries() {
        dailyEntries = entries.map {
            DailyEntry(date: $0.timestamp, speed: $0.speedMPH)
        }.sorted { $0.date < $1.date }
    }
}
