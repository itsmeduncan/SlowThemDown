package com.slowdown.shared.calculator

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpeedCalculatorTest {

    @Test
    fun calculateSpeedMPH_knownInputs() {
        // 100 pixels at 10 px/ft over 1 second = 10 ft/s ~ 6.818 MPH
        val result = SpeedCalculator.calculateSpeedMPH(
            pixelDisplacement = 100.0,
            pixelsPerFoot = 10.0,
            timeDeltaSeconds = 1.0
        )
        assertTrue(abs(result - 6.81818) < 0.001)
    }

    @Test
    fun calculateSpeedMPH_zeroPixelsPerFoot_returnsZero() {
        val result = SpeedCalculator.calculateSpeedMPH(
            pixelDisplacement = 100.0,
            pixelsPerFoot = 0.0,
            timeDeltaSeconds = 1.0
        )
        assertEquals(0.0, result)
    }

    @Test
    fun calculateSpeedMPH_zeroTimeDelta_returnsZero() {
        val result = SpeedCalculator.calculateSpeedMPH(
            pixelDisplacement = 100.0,
            pixelsPerFoot = 10.0,
            timeDeltaSeconds = 0.0
        )
        assertEquals(0.0, result)
    }

    @Test
    fun pixelsPerFoot_knownConversion() {
        val result = SpeedCalculator.pixelsPerFoot(pixelDistance = 200.0, referenceFeet = 10.0)
        assertEquals(20.0, result)
    }

    @Test
    fun pixelsPerFoot_zeroReference_returnsZero() {
        val result = SpeedCalculator.pixelsPerFoot(pixelDistance = 200.0, referenceFeet = 0.0)
        assertEquals(0.0, result)
    }

    @Test
    fun v85_emptyArray_returnsNull() {
        assertNull(SpeedCalculator.v85(emptyList()))
    }

    @Test
    fun v85_singleElement() {
        val result = SpeedCalculator.v85(listOf(30.0))
        assertEquals(30.0, result)
    }

    @Test
    fun v85_knownArray() {
        // Sorted: [20, 22, 25, 28, 30, 32, 35, 38, 40, 45]
        // rank = 0.85 * 9 = 7.65
        // lower = 7 (value 38), upper = 8 (value 40), fraction = 0.65
        // result = 38 + 0.65 * (40 - 38) = 39.3
        val speeds = listOf(30.0, 25.0, 35.0, 20.0, 40.0, 28.0, 45.0, 22.0, 32.0, 38.0)
        val result = SpeedCalculator.v85(speeds)
        assertNotNull(result)
        assertTrue(abs(result - 39.3) < 0.001)
    }

    @Test
    fun trafficStats_emptyEntries_returnsNull() {
        assertNull(SpeedCalculator.trafficStats(emptyList()))
    }

    @Test
    fun trafficStats_aggregation() {
        val entries = listOf(
            20.0 to 25,
            30.0 to 25,
            40.0 to 25,
        )
        val stats = SpeedCalculator.trafficStats(entries)
        assertNotNull(stats)
        assertEquals(3, stats.count)
        assertTrue(abs(stats.mean - 30.0) < 0.001)
        assertEquals(30.0, stats.median)
        assertEquals(20.0, stats.min)
        assertEquals(40.0, stats.max)
        assertEquals(2, stats.overLimitCount) // 30 and 40 are over 25
        assertTrue(abs(stats.overLimitPercent - 66.666) < 0.1)
    }
}
