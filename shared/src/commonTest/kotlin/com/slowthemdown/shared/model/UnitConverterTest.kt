package com.slowthemdown.shared.model

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnitConverterTest {

    // MARK: - Speed

    @Test
    fun displaySpeed_imperial_convertsToMPH() {
        // 11.176 m/s = 25 MPH
        val result = UnitConverter.displaySpeed(11.176, MeasurementSystem.IMPERIAL)
        assertTrue(abs(result - 25.0) < 0.01)
    }

    @Test
    fun displaySpeed_metric_convertsToKmh() {
        // 11.176 m/s = 40.2336 km/h
        val result = UnitConverter.displaySpeed(11.176, MeasurementSystem.METRIC)
        assertTrue(abs(result - 40.2336) < 0.01)
    }

    @Test
    fun speedToMps_imperial_convertsFromMPH() {
        val result = UnitConverter.speedToMps(25.0, MeasurementSystem.IMPERIAL)
        assertTrue(abs(result - 11.176) < 0.01)
    }

    @Test
    fun speedToMps_metric_convertsFromKmh() {
        val result = UnitConverter.speedToMps(40.2336, MeasurementSystem.METRIC)
        assertTrue(abs(result - 11.176) < 0.01)
    }

    @Test
    fun speed_roundTrip_imperial() {
        val original = 25.0
        val mps = UnitConverter.speedToMps(original, MeasurementSystem.IMPERIAL)
        val displayed = UnitConverter.displaySpeed(mps, MeasurementSystem.IMPERIAL)
        assertTrue(abs(displayed - original) < 0.001)
    }

    @Test
    fun speed_roundTrip_metric() {
        val original = 50.0
        val mps = UnitConverter.speedToMps(original, MeasurementSystem.METRIC)
        val displayed = UnitConverter.displaySpeed(mps, MeasurementSystem.METRIC)
        assertTrue(abs(displayed - original) < 0.001)
    }

    @Test
    fun speedUnit_imperial() {
        assertEquals("MPH", UnitConverter.speedUnit(MeasurementSystem.IMPERIAL))
    }

    @Test
    fun speedUnit_metric() {
        assertEquals("km/h", UnitConverter.speedUnit(MeasurementSystem.METRIC))
    }

    // MARK: - Distance

    @Test
    fun displayDistance_imperial_convertsToFeet() {
        val result = UnitConverter.displayDistance(3.048, MeasurementSystem.IMPERIAL)
        assertTrue(abs(result - 10.0) < 0.01)
    }

    @Test
    fun displayDistance_metric_passesThrough() {
        val result = UnitConverter.displayDistance(3.048, MeasurementSystem.METRIC)
        assertEquals(3.048, result)
    }

    @Test
    fun distanceToMeters_imperial_convertsFromFeet() {
        val result = UnitConverter.distanceToMeters(10.0, MeasurementSystem.IMPERIAL)
        assertTrue(abs(result - 3.048) < 0.01)
    }

    @Test
    fun distanceUnit_imperial() {
        assertEquals("ft", UnitConverter.distanceUnit(MeasurementSystem.IMPERIAL))
    }

    @Test
    fun distanceUnit_metric() {
        assertEquals("m", UnitConverter.distanceUnit(MeasurementSystem.METRIC))
    }

    // MARK: - Calibration

    @Test
    fun displayPixelsPerUnit_imperial_convertsToPixelsPerFoot() {
        // 32.8084 px/m → 10 px/ft
        val result = UnitConverter.displayPixelsPerUnit(32.8084, MeasurementSystem.IMPERIAL)
        assertTrue(abs(result - 10.0) < 0.01)
    }

    @Test
    fun displayPixelsPerUnit_metric_passesThrough() {
        val result = UnitConverter.displayPixelsPerUnit(32.8084, MeasurementSystem.METRIC)
        assertEquals(32.8084, result)
    }

    @Test
    fun calibrationUnit_imperial() {
        assertEquals("px/ft", UnitConverter.calibrationUnit(MeasurementSystem.IMPERIAL))
    }

    @Test
    fun calibrationUnit_metric() {
        assertEquals("px/m", UnitConverter.calibrationUnit(MeasurementSystem.METRIC))
    }
}
