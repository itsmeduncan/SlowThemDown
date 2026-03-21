package com.slowthemdown.android.data.db

import com.slowthemdown.shared.model.CalibrationMethod
import com.slowthemdown.shared.model.SpeedCategory
import com.slowthemdown.shared.model.TravelDirection
import com.slowthemdown.shared.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedEntryEntityTest {

    // MARK: - Enum accessors

    @Test
    fun vehicleType_parsesFromRawValue() {
        val entity = makeEntry(vehicleTypeRaw = "truck")
        assertEquals(VehicleType.TRUCK, entity.vehicleType)
    }

    @Test
    fun vehicleType_unknownRawDefaultsToCar() {
        val entity = makeEntry(vehicleTypeRaw = "segway")
        assertEquals(VehicleType.CAR, entity.vehicleType)
    }

    @Test
    fun direction_parsesFromRawValue() {
        val entity = makeEntry(directionRaw = "toward")
        assertEquals(TravelDirection.TOWARD, entity.direction)
    }

    @Test
    fun direction_unknownRawDefaultsToLeftToRight() {
        val entity = makeEntry(directionRaw = "sideways")
        assertEquals(TravelDirection.LEFT_TO_RIGHT, entity.direction)
    }

    @Test
    fun calibrationMethod_parsesFromRawValue() {
        val entity = makeEntry(calibrationMethodRaw = "vehicle_reference")
        assertEquals(CalibrationMethod.VEHICLE_REFERENCE, entity.calibrationMethod)
    }

    @Test
    fun calibrationMethod_unknownRawDefaultsToManualDistance() {
        val entity = makeEntry(calibrationMethodRaw = "laser")
        assertEquals(CalibrationMethod.MANUAL_DISTANCE, entity.calibrationMethod)
    }

    // MARK: - isOverLimit

    @Test
    fun isOverLimit_speedAboveLimit_returnsTrue() {
        // 30 MPH = 13.4112 m/s, 25 MPH = 11.176 m/s
        val entity = makeEntry(speed = 13.4112, speedLimit = 11.176)
        assertTrue(entity.isOverLimit)
    }

    @Test
    fun isOverLimit_speedAtLimit_returnsFalse() {
        val entity = makeEntry(speed = 11.176, speedLimit = 11.176)
        assertFalse(entity.isOverLimit)
    }

    @Test
    fun isOverLimit_speedBelowLimit_returnsFalse() {
        // 20 MPH = 8.9408 m/s
        val entity = makeEntry(speed = 8.9408, speedLimit = 11.176)
        assertFalse(entity.isOverLimit)
    }

    // MARK: - speedCategory

    @Test
    fun speedCategory_underLimit() {
        // 20 MPH / 25 MPH = 0.8 ratio → UNDER_LIMIT
        val entity = makeEntry(speed = 8.9408, speedLimit = 11.176)
        assertEquals(SpeedCategory.UNDER_LIMIT, entity.speedCategory)
    }

    @Test
    fun speedCategory_marginal() {
        // 28 MPH = 12.51712 m/s, 25 MPH = 11.176 m/s → ratio ~1.12 → MARGINAL
        val entity = makeEntry(speed = 12.51712, speedLimit = 11.176)
        assertEquals(SpeedCategory.MARGINAL, entity.speedCategory)
    }

    @Test
    fun speedCategory_overLimit() {
        // 35 MPH = 15.6464 m/s, 25 MPH = 11.176 m/s → ratio ~1.4 → OVER_LIMIT
        val entity = makeEntry(speed = 15.6464, speedLimit = 11.176)
        assertEquals(SpeedCategory.OVER_LIMIT, entity.speedCategory)
    }

    // MARK: - Helpers

    private fun makeEntry(
        speed: Double = 13.4112, // 30 MPH in m/s
        speedLimit: Double = 11.176, // 25 MPH in m/s
        vehicleTypeRaw: String = VehicleType.CAR.rawValue,
        directionRaw: String = TravelDirection.LEFT_TO_RIGHT.rawValue,
        calibrationMethodRaw: String = CalibrationMethod.MANUAL_DISTANCE.rawValue,
    ) = SpeedEntryEntity(
        speed = speed,
        speedLimit = speedLimit,
        vehicleTypeRaw = vehicleTypeRaw,
        directionRaw = directionRaw,
        calibrationMethodRaw = calibrationMethodRaw,
    )
}
