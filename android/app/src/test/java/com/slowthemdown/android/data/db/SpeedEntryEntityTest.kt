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
        val entity = makeEntry(speedMPH = 30.0, speedLimit = 25)
        assertTrue(entity.isOverLimit)
    }

    @Test
    fun isOverLimit_speedAtLimit_returnsFalse() {
        val entity = makeEntry(speedMPH = 25.0, speedLimit = 25)
        assertFalse(entity.isOverLimit)
    }

    @Test
    fun isOverLimit_speedBelowLimit_returnsFalse() {
        val entity = makeEntry(speedMPH = 20.0, speedLimit = 25)
        assertFalse(entity.isOverLimit)
    }

    // MARK: - speedCategory

    @Test
    fun speedCategory_underLimit() {
        val entity = makeEntry(speedMPH = 20.0, speedLimit = 25)
        assertEquals(SpeedCategory.UNDER_LIMIT, entity.speedCategory)
    }

    @Test
    fun speedCategory_marginal() {
        val entity = makeEntry(speedMPH = 28.0, speedLimit = 25)
        assertEquals(SpeedCategory.MARGINAL, entity.speedCategory)
    }

    @Test
    fun speedCategory_overLimit() {
        val entity = makeEntry(speedMPH = 35.0, speedLimit = 25)
        assertEquals(SpeedCategory.OVER_LIMIT, entity.speedCategory)
    }

    // MARK: - Helpers

    private fun makeEntry(
        speedMPH: Double = 30.0,
        speedLimit: Int = 25,
        vehicleTypeRaw: String = VehicleType.CAR.rawValue,
        directionRaw: String = TravelDirection.LEFT_TO_RIGHT.rawValue,
        calibrationMethodRaw: String = CalibrationMethod.MANUAL_DISTANCE.rawValue,
    ) = SpeedEntryEntity(
        speedMPH = speedMPH,
        speedLimit = speedLimit,
        vehicleTypeRaw = vehicleTypeRaw,
        directionRaw = directionRaw,
        calibrationMethodRaw = calibrationMethodRaw,
    )
}
