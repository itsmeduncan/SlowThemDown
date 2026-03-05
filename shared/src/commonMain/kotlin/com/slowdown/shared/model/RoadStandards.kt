package com.slowdown.shared.model

object RoadStandards {
    const val narrowLane: Double = 9.0
    const val standardLane: Double = 10.0
    const val wideLane: Double = 11.0
    const val arteryLane: Double = 12.0

    val allWidths: List<Pair<String, Double>> = listOf(
        "Narrow (9 ft)" to narrowLane,
        "Standard (10 ft)" to standardLane,
        "Wide (11 ft)" to wideLane,
        "Artery (12 ft)" to arteryLane,
    )

    val speedLimits: List<Int> = listOf(15, 20, 25, 30, 35, 40, 45)
    const val defaultSpeedLimit: Int = 25
}
