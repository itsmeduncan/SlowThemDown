package com.slowthemdown.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnumsTest {

    // MARK: - CalibrationMethod

    @Test
    fun calibrationMethod_fromRawValue_validValues() {
        assertEquals(CalibrationMethod.MANUAL_DISTANCE, CalibrationMethod.fromRawValue("manual_distance"))
        assertEquals(CalibrationMethod.VEHICLE_REFERENCE, CalibrationMethod.fromRawValue("vehicle_reference"))
    }

    @Test
    fun calibrationMethod_fromRawValue_unknownDefaultsToManualDistance() {
        assertEquals(CalibrationMethod.MANUAL_DISTANCE, CalibrationMethod.fromRawValue("bogus"))
        assertEquals(CalibrationMethod.MANUAL_DISTANCE, CalibrationMethod.fromRawValue(""))
    }

    @Test
    fun calibrationMethod_rawValueRoundtrip() {
        CalibrationMethod.entries.forEach { method ->
            assertEquals(method, CalibrationMethod.fromRawValue(method.rawValue))
        }
    }

    @Test
    fun calibrationMethod_labelsAreNotEmpty() {
        CalibrationMethod.entries.forEach { method ->
            assertTrue(method.label.isNotBlank(), "Label for $method should not be blank")
        }
    }

    // MARK: - CaptureMethod

    @Test
    fun captureMethod_fromRawValue_validValues() {
        assertEquals(CaptureMethod.CAMERA, CaptureMethod.fromRawValue("camera"))
        assertEquals(CaptureMethod.LIBRARY, CaptureMethod.fromRawValue("library"))
    }

    @Test
    fun captureMethod_fromRawValue_unknownDefaultsToCamera() {
        assertEquals(CaptureMethod.CAMERA, CaptureMethod.fromRawValue("bogus"))
    }

    @Test
    fun captureMethod_rawValueRoundtrip() {
        CaptureMethod.entries.forEach { method ->
            assertEquals(method, CaptureMethod.fromRawValue(method.rawValue))
        }
    }

    // MARK: - TravelDirection

    @Test
    fun travelDirection_fromRawValue_allValid() {
        assertEquals(TravelDirection.TOWARD, TravelDirection.fromRawValue("toward"))
        assertEquals(TravelDirection.AWAY, TravelDirection.fromRawValue("away"))
        assertEquals(TravelDirection.LEFT_TO_RIGHT, TravelDirection.fromRawValue("left_to_right"))
        assertEquals(TravelDirection.RIGHT_TO_LEFT, TravelDirection.fromRawValue("right_to_left"))
    }

    @Test
    fun travelDirection_fromRawValue_unknownDefaultsToLeftToRight() {
        assertEquals(TravelDirection.LEFT_TO_RIGHT, TravelDirection.fromRawValue("diagonal"))
    }

    @Test
    fun travelDirection_rawValueRoundtrip() {
        TravelDirection.entries.forEach { dir ->
            assertEquals(dir, TravelDirection.fromRawValue(dir.rawValue))
        }
    }

    // MARK: - VehicleType

    @Test
    fun vehicleType_fromRawValue_allValid() {
        assertEquals(VehicleType.CAR, VehicleType.fromRawValue("car"))
        assertEquals(VehicleType.SUV, VehicleType.fromRawValue("suv"))
        assertEquals(VehicleType.TRUCK, VehicleType.fromRawValue("truck"))
        assertEquals(VehicleType.VAN, VehicleType.fromRawValue("van"))
        assertEquals(VehicleType.MOTORCYCLE, VehicleType.fromRawValue("motorcycle"))
        assertEquals(VehicleType.BUS, VehicleType.fromRawValue("bus"))
        assertEquals(VehicleType.OTHER, VehicleType.fromRawValue("other"))
    }

    @Test
    fun vehicleType_fromRawValue_unknownDefaultsToCar() {
        assertEquals(VehicleType.CAR, VehicleType.fromRawValue("bicycle"))
        assertEquals(VehicleType.CAR, VehicleType.fromRawValue(""))
    }

    @Test
    fun vehicleType_rawValueRoundtrip() {
        VehicleType.entries.forEach { type ->
            assertEquals(type, VehicleType.fromRawValue(type.rawValue))
        }
    }

    // MARK: - SpeedCategory (uses Double for both speed and limit now)

    @Test
    fun speedCategory_underLimit() {
        assertEquals(SpeedCategory.UNDER_LIMIT, SpeedCategory.fromSpeed(8.94, 11.176))
        assertEquals(SpeedCategory.UNDER_LIMIT, SpeedCategory.fromSpeed(11.176, 11.176))
    }

    @Test
    fun speedCategory_marginal() {
        assertEquals(SpeedCategory.MARGINAL, SpeedCategory.fromSpeed(11.62, 11.176))
        assertEquals(SpeedCategory.MARGINAL, SpeedCategory.fromSpeed(13.41, 11.176)) // ratio = 1.2
    }

    @Test
    fun speedCategory_overLimit() {
        assertEquals(SpeedCategory.OVER_LIMIT, SpeedCategory.fromSpeed(13.87, 11.176)) // ratio > 1.2
        assertEquals(SpeedCategory.OVER_LIMIT, SpeedCategory.fromSpeed(22.35, 11.176))
    }

    @Test
    fun speedCategory_labelsAreNotEmpty() {
        SpeedCategory.entries.forEach { cat ->
            assertTrue(cat.label.isNotBlank(), "Label for $cat should not be blank")
        }
    }
}
