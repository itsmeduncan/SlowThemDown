package com.slowthemdown.shared.calculator

import com.slowthemdown.shared.model.TrafficStats
import kotlin.math.sqrt

object SpeedCalculator {
    /**
     * Calculate speed in m/s from pixel displacement, pixels-per-meter, and time delta.
     * Formula: (pixelDisplacement / pixelsPerMeter) meters / timeDeltaSeconds seconds → m/s
     */
    fun calculateSpeed(
        pixelDisplacement: Double,
        pixelsPerMeter: Double,
        timeDeltaSeconds: Double
    ): Double {
        if (pixelsPerMeter <= 0 || timeDeltaSeconds <= 0) return 0.0
        val distanceMeters = pixelDisplacement / pixelsPerMeter
        return distanceMeters / timeDeltaSeconds
    }

    /** Calculate pixels per meter from a known reference distance in meters */
    fun pixelsPerMeter(pixelDistance: Double, referenceMeters: Double): Double {
        if (referenceMeters <= 0) return 0.0
        return pixelDistance / referenceMeters
    }

    /** Compute V85 — the interpolated 85th percentile speed */
    fun v85(speeds: List<Double>): Double? {
        if (speeds.isEmpty()) return null
        val sorted = speeds.sorted()
        val n = sorted.size.toDouble()
        val rank = 0.85 * (n - 1)
        val lower = rank.toInt()
        val upper = minOf(lower + 1, sorted.size - 1)
        val fraction = rank - lower.toDouble()
        return sorted[lower] + fraction * (sorted[upper] - sorted[lower])
    }

    /**
     * Compute traffic statistics for a set of speed/limit pairs.
     * Each pair is (speed in m/s, speedLimit in m/s).
     */
    fun trafficStats(entries: List<Pair<Double, Double>>): TrafficStats? {
        if (entries.isEmpty()) return null
        val speeds = entries.map { it.first }
        val sorted = speeds.sorted()
        val mean = speeds.sum() / speeds.size

        val overLimit = entries.count { it.first > it.second }
        val overPercent = overLimit.toDouble() / entries.size * 100

        return TrafficStats(
            count = entries.size,
            mean = mean,
            median = median(sorted),
            min = sorted.first(),
            max = sorted.last(),
            v85 = v85(speeds) ?: 0.0,
            overLimitCount = overLimit,
            overLimitPercent = overPercent,
            standardDeviation = standardDeviation(speeds, mean)
        )
    }

    private fun median(sorted: List<Double>): Double {
        if (sorted.isEmpty()) return 0.0
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2
        } else {
            sorted[mid]
        }
    }

    private fun standardDeviation(values: List<Double>, mean: Double): Double {
        if (values.size <= 1) return 0.0
        val variance = values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
        return sqrt(variance)
    }
}
