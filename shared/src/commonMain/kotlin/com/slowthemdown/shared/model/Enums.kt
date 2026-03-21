package com.slowthemdown.shared.model

enum class CalibrationMethod(val rawValue: String, val label: String) {
    MANUAL_DISTANCE("manual_distance", "Manual Distance"),
    VEHICLE_REFERENCE("vehicle_reference", "Vehicle Reference");

    companion object {
        fun fromRawValue(raw: String): CalibrationMethod =
            entries.firstOrNull { it.rawValue == raw } ?: MANUAL_DISTANCE
    }
}

enum class CaptureMethod(val rawValue: String, val label: String) {
    CAMERA("camera", "Record Video"),
    LIBRARY("library", "Import from Library");

    companion object {
        fun fromRawValue(raw: String): CaptureMethod =
            entries.firstOrNull { it.rawValue == raw } ?: CAMERA
    }
}

enum class TravelDirection(val rawValue: String, val label: String) {
    TOWARD("toward", "Toward Camera"),
    AWAY("away", "Away from Camera"),
    LEFT_TO_RIGHT("left_to_right", "Left to Right"),
    RIGHT_TO_LEFT("right_to_left", "Right to Left");

    companion object {
        fun fromRawValue(raw: String): TravelDirection =
            entries.firstOrNull { it.rawValue == raw } ?: LEFT_TO_RIGHT
    }
}

enum class VehicleType(val rawValue: String, val label: String) {
    CAR("car", "Car"),
    SUV("suv", "SUV"),
    TRUCK("truck", "Truck"),
    VAN("van", "Van"),
    MOTORCYCLE("motorcycle", "Motorcycle"),
    BUS("bus", "Bus"),
    OTHER("other", "Other");

    companion object {
        fun fromRawValue(raw: String): VehicleType =
            entries.firstOrNull { it.rawValue == raw } ?: CAR
    }
}

enum class SpeedCategory(val label: String) {
    UNDER_LIMIT("Under Limit"),
    MARGINAL("Marginal"),
    OVER_LIMIT("Over Limit");

    companion object {
        /** Categorize speed relative to limit. Both values must be in the same unit (e.g. m/s). */
        fun fromSpeed(speed: Double, speedLimit: Double): SpeedCategory {
            if (speedLimit <= 0.0) return UNDER_LIMIT
            val ratio = speed / speedLimit
            return when {
                ratio <= 1.0 -> UNDER_LIMIT
                ratio <= 1.2 -> MARGINAL
                else -> OVER_LIMIT
            }
        }
    }
}

enum class VehicleCategory(val label: String) {
    SEDAN("Sedan"),
    SUV("SUV"),
    TRUCK("Truck"),
    VAN("Van"),
    COMPACT("Compact")
}
