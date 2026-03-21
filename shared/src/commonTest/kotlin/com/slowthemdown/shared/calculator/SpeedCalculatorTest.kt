package com.slowthemdown.shared.calculator

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeedCalculatorTest {

    @Test
    fun calculateSpeed_knownInputs() {
        // 100 pixels at 10 px/m over 1 second = 10 m/s
        val result = SpeedCalculator.calculateSpeed(
            pixelDisplacement = 100.0,
            pixelsPerMeter = 10.0,
            timeDeltaSeconds = 1.0
        )
        assertTrue(abs(result - 10.0) < 0.001)
    }

    @Test
    fun calculateSpeed_zeroPixelsPerMeter_returnsZero() {
        val result = SpeedCalculator.calculateSpeed(
            pixelDisplacement = 100.0,
            pixelsPerMeter = 0.0,
            timeDeltaSeconds = 1.0
        )
        assertEquals(0.0, result)
    }

    @Test
    fun calculateSpeed_zeroTimeDelta_returnsZero() {
        val result = SpeedCalculator.calculateSpeed(
            pixelDisplacement = 100.0,
            pixelsPerMeter = 10.0,
            timeDeltaSeconds = 0.0
        )
        assertEquals(0.0, result)
    }

    @Test
    fun pixelsPerMeter_knownConversion() {
        val result = SpeedCalculator.pixelsPerMeter(pixelDistance = 200.0, referenceMeters = 10.0)
        assertEquals(20.0, result)
    }

    @Test
    fun pixelsPerMeter_zeroReference_returnsZero() {
        val result = SpeedCalculator.pixelsPerMeter(pixelDistance = 200.0, referenceMeters = 0.0)
        assertEquals(0.0, result)
    }

    @Test
    fun v85_emptyArray_returnsNull() {
        assertNull(SpeedCalculator.v85(emptyList()))
    }

    @Test
    fun v85_singleElement() {
        val result = SpeedCalculator.v85(listOf(13.41))
        assertEquals(13.41, result)
    }

    @Test
    fun v85_knownArray() {
        // Sorted: [8.94, 9.83, 11.18, 12.52, 13.41, 14.31, 15.65, 16.99, 17.88, 20.12]
        // rank = 0.85 * 9 = 7.65
        // lower = 7 (value 16.99), upper = 8 (value 17.88), fraction = 0.65
        // result = 16.99 + 0.65 * (17.88 - 16.99) = 17.5685
        val speeds = listOf(13.41, 11.18, 15.65, 8.94, 17.88, 12.52, 20.12, 9.83, 14.31, 16.99)
        val result = SpeedCalculator.v85(speeds)
        assertNotNull(result)
        assertTrue(abs(result - 17.5685) < 0.001)
    }

    @Test
    fun trafficStats_emptyEntries_returnsNull() {
        assertNull(SpeedCalculator.trafficStats(emptyList()))
    }

    @Test
    fun trafficStats_aggregation() {
        // Speeds in m/s, limit in m/s (11.176 = 25 MPH)
        val limit = 11.176
        val entries = listOf(
            8.94 to limit,   // ~20 MPH, under
            13.41 to limit,  // ~30 MPH, over
            17.88 to limit,  // ~40 MPH, over
        )
        val stats = SpeedCalculator.trafficStats(entries)
        assertNotNull(stats)
        assertEquals(3, stats.count)
        assertTrue(abs(stats.mean - 13.41) < 0.01)
        assertEquals(13.41, stats.median)
        assertEquals(8.94, stats.min)
        assertEquals(17.88, stats.max)
        assertEquals(2, stats.overLimitCount) // 13.41 and 17.88 are over 11.176
        assertTrue(abs(stats.overLimitPercent - 66.666) < 0.1)
    }
}
