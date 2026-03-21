package com.slowthemdown.shared.model

enum class MeasurementSystem(val rawValue: String) {
    IMPERIAL("imperial"),
    METRIC("metric");

    companion object {
        fun fromRawValue(raw: String): MeasurementSystem =
            entries.firstOrNull { it.rawValue == raw } ?: IMPERIAL
    }
}

object UnitConverter {
    // Constants
    private const val MPS_TO_MPH = 2.23694
    private const val MPS_TO_KMH = 3.6
    private const val METERS_TO_FEET = 3.28084

    // Speed: m/s → display unit
    fun displaySpeed(mps: Double, system: MeasurementSystem): Double = when (system) {
        MeasurementSystem.IMPERIAL -> mps * MPS_TO_MPH
        MeasurementSystem.METRIC -> mps * MPS_TO_KMH
    }

    // Display unit → m/s
    fun speedToMps(value: Double, system: MeasurementSystem): Double = when (system) {
        MeasurementSystem.IMPERIAL -> value / MPS_TO_MPH
        MeasurementSystem.METRIC -> value / MPS_TO_KMH
    }

    fun speedUnit(system: MeasurementSystem): String = when (system) {
        MeasurementSystem.IMPERIAL -> "MPH"
        MeasurementSystem.METRIC -> "km/h"
    }

    // Distance: meters → display unit
    fun displayDistance(meters: Double, system: MeasurementSystem): Double = when (system) {
        MeasurementSystem.IMPERIAL -> meters * METERS_TO_FEET
        MeasurementSystem.METRIC -> meters
    }

    // Display unit → meters
    fun distanceToMeters(value: Double, system: MeasurementSystem): Double = when (system) {
        MeasurementSystem.IMPERIAL -> value / METERS_TO_FEET
        MeasurementSystem.METRIC -> value
    }

    fun distanceUnit(system: MeasurementSystem): String = when (system) {
        MeasurementSystem.IMPERIAL -> "ft"
        MeasurementSystem.METRIC -> "m"
    }

    // Calibration ratio: pixels per meter → display
    fun displayPixelsPerUnit(ppm: Double, system: MeasurementSystem): Double = when (system) {
        MeasurementSystem.IMPERIAL -> ppm / METERS_TO_FEET // px/m → px/ft
        MeasurementSystem.METRIC -> ppm
    }

    fun calibrationUnit(system: MeasurementSystem): String = when (system) {
        MeasurementSystem.IMPERIAL -> "px/ft"
        MeasurementSystem.METRIC -> "px/m"
    }
}
