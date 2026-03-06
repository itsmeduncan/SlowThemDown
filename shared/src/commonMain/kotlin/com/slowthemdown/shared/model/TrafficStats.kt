package com.slowthemdown.shared.model

data class TrafficStats(
    val count: Int,
    val mean: Double,
    val median: Double,
    val min: Double,
    val max: Double,
    val v85: Double,
    val overLimitCount: Int,
    val overLimitPercent: Double,
    val standardDeviation: Double
)
