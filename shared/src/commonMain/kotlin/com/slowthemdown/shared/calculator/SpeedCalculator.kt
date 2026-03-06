package com.slowthemdown.shared.calculator

import com.slowthemdown.shared.model.TrafficStats
import kotlin.math.sqrt

object SpeedCalculator {
    /**
     * Calculate speed in MPH from pixel displacement, pixels-per-foot, and time delta.
     * Formula: (pixelDisplacement / pixelsPerFoot) feet / timeDeltaSeconds seconds -> MPH
     */
    fun calculateSpeedMPH(
        pixelDisplacement: Double,
        pixelsPerFoot: Double,
        timeDeltaSeconds: Double
    ): Double {
        if (pixelsPerFoot <= 0 || timeDeltaSeconds <= 0) return 0.0
        val distanceFeet = pixelDisplacement / pixelsPerFoot
        val feetPerSecond = distanceFeet / timeDeltaSeconds
        return feetPerSecond * 0.681818 // ft/s to MPH
    }

    /** Calculate pixels per foot from a known reference distance */
    fun pixelsPerFoot(pixelDistance: Double, referenceFeet: Double): Double {
        if (referenceFeet <= 0) return 0.0
        return pixelDistance / referenceFeet
    }

    /** Compute V85 - the interpolated 85th percentile speed */
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
     * Each pair is (speedMPH, speedLimit).
     */
    fun trafficStats(entries: List<Pair<Double, Int>>): TrafficStats? {
        if (entries.isEmpty()) return null
        val speeds = entries.map { it.first }
        val sorted = speeds.sorted()
        val mean = speeds.sum() / speeds.size

        val overLimit = entries.count { it.first > it.second.toDouble() }
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
