package com.slowthemdown.shared.model

object RoadStandards {
    /** Standard residential lane widths in meters */
    const val narrowLane: Double = 2.7432
    const val standardLane: Double = 3.048
    const val wideLane: Double = 3.3528
    const val arteryLane: Double = 3.6576

    /** Lane widths stored in meters; labels generated at display time */
    val allWidths: List<Pair<String, Double>> = listOf(
        "narrow" to narrowLane,
        "standard" to standardLane,
        "wide" to wideLane,
        "artery" to arteryLane,
    )

    /** Returns a display label for a lane width in the user's measurement system */
    fun laneLabel(key: String, meters: Double, system: MeasurementSystem): String {
        val displayName = key.replaceFirstChar { it.uppercase() }
        return when (system) {
            MeasurementSystem.IMPERIAL -> {
                val feet = meters * 3.28084
                "$displayName (${feet.toInt()} ft)"
            }
            MeasurementSystem.METRIC -> {
                val rounded = (meters * 10).toInt() / 10.0
                "$displayName ($rounded m)"
            }
        }
    }

    /** Imperial speed limits as m/s values (correspond to round MPH values) */
    val imperialSpeedLimits: List<Double> = listOf(15, 20, 25, 30, 35, 40, 45).map { it * 0.44704 }

    /** Metric speed limits as m/s values (correspond to round km/h values) */
    val metricSpeedLimits: List<Double> = listOf(20, 30, 40, 50, 60, 70, 80).map { it / 3.6 }

    /** Returns speed limit options in m/s for the user's measurement system */
    fun speedLimitsForSystem(system: MeasurementSystem): List<Double> = when (system) {
        MeasurementSystem.IMPERIAL -> imperialSpeedLimits
        MeasurementSystem.METRIC -> metricSpeedLimits
    }

    /** Default speed limit: 25 MPH = 11.176 m/s */
    const val defaultSpeedLimit: Double = 11.176
}
