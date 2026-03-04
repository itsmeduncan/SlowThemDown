import Foundation

enum SpeedCalculator {
    /// Calculate speed in MPH from pixel displacement, pixels-per-foot, and time delta
    /// Formula: (pixelDisplacement / pixelsPerFoot) feet / timeDelta seconds → MPH
    static func calculateSpeedMPH(
        pixelDisplacement: Double,
        pixelsPerFoot: Double,
        timeDeltaSeconds: Double
    ) -> Double {
        guard pixelsPerFoot > 0, timeDeltaSeconds > 0 else { return 0 }
        let distanceFeet = pixelDisplacement / pixelsPerFoot
        let feetPerSecond = distanceFeet / timeDeltaSeconds
        return feetPerSecond * 0.681818 // ft/s to MPH
    }

    /// Calculate pixels per foot from a known reference distance
    static func pixelsPerFoot(pixelDistance: Double, referenceFeet: Double) -> Double {
        guard referenceFeet > 0 else { return 0 }
        return pixelDistance / referenceFeet
    }

    /// Compute V85 — the interpolated 85th percentile speed
    static func v85(speeds: [Double]) -> Double? {
        guard !speeds.isEmpty else { return nil }
        let sorted = speeds.sorted()
        let n = Double(sorted.count)
        let rank = 0.85 * (n - 1)
        let lower = Int(floor(rank))
        let upper = min(lower + 1, sorted.count - 1)
        let fraction = rank - Double(lower)
        return sorted[lower] + fraction * (sorted[upper] - sorted[lower])
    }

    /// Compute traffic statistics for a set of speed entries
    static func trafficStats(entries: [SpeedEntry]) -> TrafficStats? {
        guard !entries.isEmpty else { return nil }
        let speeds = entries.map(\.speedMPH)
        let sorted = speeds.sorted()
        let mean = speeds.reduce(0, +) / Double(speeds.count)

        let overLimit = entries.filter(\.isOverLimit).count
        let overPercent = Double(overLimit) / Double(entries.count) * 100

        return TrafficStats(
            count: entries.count,
            mean: mean,
            median: median(sorted),
            min: sorted.first ?? 0,
            max: sorted.last ?? 0,
            v85: v85(speeds: speeds) ?? 0,
            overLimitCount: overLimit,
            overLimitPercent: overPercent,
            standardDeviation: standardDeviation(speeds, mean: mean)
        )
    }

    private static func median(_ sorted: [Double]) -> Double {
        guard !sorted.isEmpty else { return 0 }
        let mid = sorted.count / 2
        if sorted.count.isMultiple(of: 2) {
            return (sorted[mid - 1] + sorted[mid]) / 2
        }
        return sorted[mid]
    }

    private static func standardDeviation(_ values: [Double], mean: Double) -> Double {
        guard values.count > 1 else { return 0 }
        let variance = values.reduce(0) { $0 + ($1 - mean) * ($1 - mean) } / Double(values.count - 1)
        return sqrt(variance)
    }
}

struct TrafficStats {
    let count: Int
    let mean: Double
    let median: Double
    let min: Double
    let max: Double
    let v85: Double
    let overLimitCount: Int
    let overLimitPercent: Double
    let standardDeviation: Double
}
