package com.slowthemdown.android.data.db

import com.slowthemdown.android.data.datastore.Calibration
import com.slowthemdown.shared.model.CalibrationMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationTest {

    @Test
    fun isValid_positivePixelsPerMeter_returnsTrue() {
        val cal = Calibration(pixelsPerMeter = 10.0)
        assertTrue(cal.isValid)
    }

    @Test
    fun isValid_zeroPixelsPerMeter_returnsFalse() {
        val cal = Calibration()
        assertFalse(cal.isValid)
    }

    @Test
    fun isValid_negativePixelsPerMeter_returnsFalse() {
        val cal = Calibration(pixelsPerMeter = -1.0)
        assertFalse(cal.isValid)
    }

    @Test
    fun defaultValues() {
        val cal = Calibration()
        assertEquals(CalibrationMethod.MANUAL_DISTANCE, cal.method)
        assertEquals(0.0, cal.pixelsPerMeter, 0.001)
        assertEquals(0.0, cal.referenceDistanceMeters, 0.001)
        assertEquals(0.0, cal.pixelDistance, 0.001)
        assertEquals(null, cal.vehicleReferenceName)
    }

    @Test
    fun customValues_preserved() {
        val cal = Calibration(
            method = CalibrationMethod.VEHICLE_REFERENCE,
            pixelsPerMeter = 50.85, // ~15.5 px/ft * 3.28084
            referenceDistanceMeters = 4.877, // ~16 ft
            pixelDistance = 248.0,
            vehicleReferenceName = "Toyota Camry",
        )
        assertEquals(CalibrationMethod.VEHICLE_REFERENCE, cal.method)
        assertEquals(50.85, cal.pixelsPerMeter, 0.001)
        assertEquals(4.877, cal.referenceDistanceMeters, 0.001)
        assertEquals(248.0, cal.pixelDistance, 0.001)
        assertEquals("Toyota Camry", cal.vehicleReferenceName)
    }
}
