package com.slowthemdown.android.viewmodel

import android.app.Application
import com.slowthemdown.android.data.AgencyDirectory
import com.slowthemdown.android.data.datastore.CalibrationStore
import com.slowthemdown.android.data.db.SpeedEntryDao
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.android.debug.SeedData
import com.slowthemdown.android.service.LocationService
import com.slowthemdown.android.service.ReportExporter
import com.slowthemdown.shared.model.MeasurementSystem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dao = mockk<SpeedEntryDao>()
    private val exporter = mockk<ReportExporter>(relaxed = true)
    private val agencyDirectory = mockk<AgencyDirectory>(relaxed = true)
    private val locationService = mockk<LocationService>(relaxed = true)
    private val calibrationStore = mockk<CalibrationStore>()
    private val application = mockk<Application>(relaxed = true)

    private val entriesFlow = MutableStateFlow<List<SpeedEntryEntity>>(emptyList())
    private val streetsFlow = MutableStateFlow<List<String>>(emptyList())

    private lateinit var vm: ReportViewModel

    private fun makeEntry(
        speed: Double,
        speedLimit: Double = 11.176,
        streetName: String = "",
        timestamp: Long = System.currentTimeMillis(),
    ): SpeedEntryEntity = SpeedEntryEntity(
        speed = speed,
        speedLimit = speedLimit,
        streetName = streetName,
        timestamp = timestamp,
    )

    private fun timestampAtHour(hour: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { dao.getAllEntries() } returns entriesFlow
        every { dao.getDistinctStreets() } returns streetsFlow
        every { calibrationStore.measurementSystem } returns flowOf(MeasurementSystem.IMPERIAL)
        mockkObject(SeedData)
        every { SeedData.isSeeded(any()) } returns false
        vm = ReportViewModel(dao, exporter, agencyDirectory, locationService, calibrationStore, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(SeedData)
    }

    // MARK: - Empty state

    @Test
    fun emptyEntries_statsIsNull() = runTest {
        entriesFlow.value = emptyList()
        assertNull(vm.stats.first())
    }

    @Test
    fun emptyEntries_histogramIsEmpty() = runTest {
        entriesFlow.value = emptyList()
        assertTrue(vm.histogram.first().isEmpty())
    }

    @Test
    fun emptyEntries_hourlyAveragesIsEmpty() = runTest {
        entriesFlow.value = emptyList()
        assertTrue(vm.hourlyAverages.first().isEmpty())
    }

    // MARK: - Stats computation

    @Test
    fun stats_computesCountAndMean() = runTest {
        entriesFlow.value = listOf(
            makeEntry(speed = 10.0),
            makeEntry(speed = 12.0),
            makeEntry(speed = 14.0),
        )
        val stats = vm.stats.first()
        assertEquals(3, stats!!.count)
        assertEquals(12.0, stats.mean, 0.001)
    }

    // MARK: - Histogram

    @Test
    fun histogram_totalCountMatchesEntries() = runTest {
        val entries = listOf(
            makeEntry(speed = 10.0),
            makeEntry(speed = 12.0),
            makeEntry(speed = 14.0),
            makeEntry(speed = 16.0),
        )
        entriesFlow.value = entries
        val buckets = vm.histogram.first()
        val totalCount = buckets.sumOf { it.count }
        assertEquals(entries.size, totalCount)
    }

    @Test
    fun histogram_singleSpeed_oneBucket() = runTest {
        entriesFlow.value = listOf(makeEntry(speed = 10.0))
        val buckets = vm.histogram.first()
        assertEquals(1, buckets.size)
        assertEquals(1, buckets[0].count)
    }

    @Test
    fun histogram_multipleSpeeds_hasBuckets() = runTest {
        entriesFlow.value = listOf(
            makeEntry(speed = 10.0),
            makeEntry(speed = 15.0),
        )
        val buckets = vm.histogram.first()
        assertTrue(buckets.size >= 2)
    }

    // MARK: - Hourly averages

    @Test
    fun hourlyAverages_groupsByHour() = runTest {
        entriesFlow.value = listOf(
            makeEntry(speed = 10.0, timestamp = timestampAtHour(8)),
            makeEntry(speed = 14.0, timestamp = timestampAtHour(8)),
            makeEntry(speed = 20.0, timestamp = timestampAtHour(17)),
        )
        val averages = vm.hourlyAverages.first()
        assertEquals(2, averages.size)

        val hour8 = averages.first { it.hour == 8 }
        assertEquals(12.0, hour8.averageSpeed, 0.001)
        assertEquals("8am", hour8.label)

        val hour17 = averages.first { it.hour == 17 }
        assertEquals(20.0, hour17.averageSpeed, 0.001)
        assertEquals("5pm", hour17.label)
    }

    @Test
    fun hourlyAverages_sortedByHour() = runTest {
        entriesFlow.value = listOf(
            makeEntry(speed = 10.0, timestamp = timestampAtHour(17)),
            makeEntry(speed = 12.0, timestamp = timestampAtHour(8)),
            makeEntry(speed = 14.0, timestamp = timestampAtHour(12)),
        )
        val averages = vm.hourlyAverages.first()
        assertEquals(3, averages.size)
        assertEquals(8, averages[0].hour)
        assertEquals(12, averages[1].hour)
        assertEquals(17, averages[2].hour)
    }

    // MARK: - Scatter points

    @Test
    fun scatterPoints_matchesInputCount() = runTest {
        val entries = listOf(
            makeEntry(speed = 10.0),
            makeEntry(speed = 12.0),
            makeEntry(speed = 14.0),
        )
        entriesFlow.value = entries
        assertEquals(entries.size, vm.scatterPoints.first().size)
    }

    @Test
    fun scatterPoints_preservesOrder() = runTest {
        val t1 = timestampAtHour(8)
        val t2 = timestampAtHour(12)
        val t3 = timestampAtHour(17)
        entriesFlow.value = listOf(
            makeEntry(speed = 10.0, timestamp = t1),
            makeEntry(speed = 12.0, timestamp = t2),
            makeEntry(speed = 14.0, timestamp = t3),
        )
        val points = vm.scatterPoints.first()
        assertEquals(t1, points[0].timestampMillis)
        assertEquals(t2, points[1].timestampMillis)
        assertEquals(t3, points[2].timestampMillis)
    }

    // MARK: - Available streets

    @Test
    fun availableStreets_reflectsDao() = runTest {
        streetsFlow.value = listOf("Oak St", "Elm Ave")
        assertEquals(listOf("Oak St", "Elm Ave"), vm.availableStreets.first())
    }

    // MARK: - Street selection and filtering

    @Test
    fun selectStreet_filtersStats() = runTest {
        entriesFlow.value = listOf(
            makeEntry(speed = 10.0, streetName = "Oak St"),
            makeEntry(speed = 20.0, streetName = "Elm Ave"),
            makeEntry(speed = 30.0, streetName = "Oak St"),
        )
        vm.selectStreet("Oak St")
        val stats = vm.stats.first()
        assertEquals(2, stats!!.count)
        assertEquals(20.0, stats.mean, 0.001)
    }

    @Test
    fun selectStreet_null_showsAll() = runTest {
        entriesFlow.value = listOf(
            makeEntry(speed = 10.0, streetName = "Oak St"),
            makeEntry(speed = 20.0, streetName = "Elm Ave"),
        )
        vm.selectStreet("Oak St")
        assertEquals(1, vm.stats.first()!!.count)

        vm.selectStreet(null)
        assertEquals(2, vm.stats.first()!!.count)
    }

    @Test
    fun filteredEntries_matchesSelectedStreet() = runTest {
        entriesFlow.value = listOf(
            makeEntry(speed = 10.0, streetName = "Oak St"),
            makeEntry(speed = 20.0, streetName = "Elm Ave"),
            makeEntry(speed = 30.0, streetName = "Oak St"),
        )
        vm.selectStreet("Oak St")
        val filtered = vm.entries.first()
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.streetName == "Oak St" })
    }

    // MARK: - Street groups

    @Test
    fun streetGroups_sortedByCount() = runTest {
        entriesFlow.value = listOf(
            makeEntry(speed = 10.0, streetName = "Oak St"),
            makeEntry(speed = 12.0, streetName = "Oak St"),
            makeEntry(speed = 14.0, streetName = "Oak St"),
            makeEntry(speed = 16.0, streetName = "Elm Ave"),
        )
        val groups = vm.streetGroups.first()
        assertEquals(2, groups.size)
        assertEquals("Oak St", groups[0].name)
        assertEquals(3, groups[0].count)
        assertEquals("Elm Ave", groups[1].name)
        assertEquals(1, groups[1].count)
    }

    @Test
    fun streetGroups_computesMeanAndOverLimitPercent() = runTest {
        entriesFlow.value = listOf(
            makeEntry(speed = 10.0, streetName = "Oak St"),
            makeEntry(speed = 12.0, streetName = "Oak St"),
            makeEntry(speed = 14.0, streetName = "Oak St"),
        )
        val groups = vm.streetGroups.first()
        assertEquals(1, groups.size)
        assertEquals(12.0, groups[0].meanSpeed, 0.001)
        assertEquals(66.667, groups[0].overLimitPercent, 0.1)
    }

    @Test
    fun streetGroups_excludesEmptyStreetNames() = runTest {
        entriesFlow.value = listOf(
            makeEntry(speed = 10.0, streetName = "Oak St"),
            makeEntry(speed = 12.0, streetName = ""),
            makeEntry(speed = 14.0, streetName = ""),
        )
        val groups = vm.streetGroups.first()
        assertEquals(1, groups.size)
        assertEquals("Oak St", groups[0].name)
    }
}
