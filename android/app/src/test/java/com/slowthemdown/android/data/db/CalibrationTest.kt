package com.slowthemdown.android.data.db

import com.slowthemdown.android.data.datastore.Calibration
import com.slowthemdown.shared.model.CalibrationMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationTest {

    @Test
    fun isValid_positivePixelsPerFoot_returnsTrue() {
        val cal = Calibration(pixelsPerFoot = 10.0)
        assertTrue(cal.isValid)
    }

    @Test
    fun isValid_zeroPixelsPerFoot_returnsFalse() {
        val cal = Calibration()
        assertFalse(cal.isValid)
    }

    @Test
    fun isValid_negativePixelsPerFoot_returnsFalse() {
        val cal = Calibration(pixelsPerFoot = -1.0)
        assertFalse(cal.isValid)
    }

    @Test
    fun defaultValues() {
        val cal = Calibration()
        assertEquals(CalibrationMethod.MANUAL_DISTANCE, cal.method)
        assertEquals(0.0, cal.pixelsPerFoot, 0.001)
        assertEquals(0.0, cal.referenceDistanceFeet, 0.001)
        assertEquals(0.0, cal.pixelDistance, 0.001)
        assertEquals(null, cal.vehicleReferenceName)
    }

    @Test
    fun customValues_preserved() {
        val cal = Calibration(
            method = CalibrationMethod.VEHICLE_REFERENCE,
            pixelsPerFoot = 15.5,
            referenceDistanceFeet = 16.0,
            pixelDistance = 248.0,
            vehicleReferenceName = "Toyota Camry",
        )
        assertEquals(CalibrationMethod.VEHICLE_REFERENCE, cal.method)
        assertEquals(15.5, cal.pixelsPerFoot, 0.001)
        assertEquals(16.0, cal.referenceDistanceFeet, 0.001)
        assertEquals(248.0, cal.pixelDistance, 0.001)
        assertEquals("Toyota Camry", cal.vehicleReferenceName)
    }
}
