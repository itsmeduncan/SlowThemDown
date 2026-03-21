package com.slowthemdown.android.service

import android.content.Context
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.shared.model.CalibrationMethod
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.TravelDirection
import com.slowthemdown.shared.model.VehicleType
import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportExporterTest {

    private val cacheDir = File(System.getProperty("java.io.tmpdir"), "test_cache_${System.nanoTime()}").also {
        it.mkdirs()
    }
    private val context = mockk<Context> {
        every { this@mockk.cacheDir } returns this@ReportExporterTest.cacheDir
    }
    private val exporter = ReportExporter(context)

    // MARK: - CSV

    @Test
    fun generateCsv_emptyEntries_hasHeaderOnly() {
        val file = exporter.generateCsvFile(emptyList(), MeasurementSystem.IMPERIAL)
        val lines = file.readLines()
        assertEquals(1, lines.size)
        assertTrue(lines[0].startsWith("Timestamp,Speed"))
        assertTrue(lines[0].contains("MPH"))
    }

    @Test
    fun generateCsv_singleEntry_hasTwoLines() {
        // 35 MPH = 15.6464 m/s, 25 MPH = 11.176 m/s
        val entries = listOf(makeEntry(speed = 15.6464, speedLimit = 11.176))
        val file = exporter.generateCsvFile(entries, MeasurementSystem.IMPERIAL)
        val lines = file.readLines()
        assertEquals(2, lines.size)
        assertTrue(lines[1].contains("35.0"))
        assertTrue(lines[1].contains("Yes")) // over limit
    }

    @Test
    fun generateCsv_metricUnits_showsKmh() {
        val file = exporter.generateCsvFile(emptyList(), MeasurementSystem.METRIC)
        val lines = file.readLines()
        assertTrue(lines[0].contains("km/h"))
    }

    @Test
    fun generateCsv_escapesCommasInStreetName() {
        val entries = listOf(makeEntry(streetName = "Main St, Suite 100"))
        val file = exporter.generateCsvFile(entries, MeasurementSystem.IMPERIAL)
        val content = file.readText()
        assertTrue(content.contains("\"Main St, Suite 100\""))
    }

    @Test
    fun generateCsv_escapesQuotesInNotes() {
        val entries = listOf(makeEntry(notes = "Said \"hello\""))
        val file = exporter.generateCsvFile(entries, MeasurementSystem.IMPERIAL)
        val content = file.readText()
        assertTrue(content.contains("\"Said \"\"hello\"\"\""))
    }

    @Test
    fun generateCsv_multipleEntries_allPresent() {
        val entries = listOf(
            makeEntry(speed = 8.9408),  // 20 MPH
            makeEntry(speed = 13.4112), // 30 MPH
            makeEntry(speed = 17.8816), // 40 MPH
        )
        val file = exporter.generateCsvFile(entries, MeasurementSystem.IMPERIAL)
        val lines = file.readLines()
        assertEquals(4, lines.size) // header + 3 entries
    }

    // MARK: - Helpers

    private fun makeEntry(
        speed: Double = 13.4112, // 30 MPH in m/s
        speedLimit: Double = 11.176, // 25 MPH in m/s
        streetName: String = "Test St",
        notes: String = "",
    ) = SpeedEntryEntity(
        speed = speed,
        speedLimit = speedLimit,
        streetName = streetName,
        notes = notes,
        vehicleTypeRaw = VehicleType.CAR.rawValue,
        directionRaw = TravelDirection.LEFT_TO_RIGHT.rawValue,
        calibrationMethodRaw = CalibrationMethod.MANUAL_DISTANCE.rawValue,
    )
}
