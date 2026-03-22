package com.slowthemdown.android.viewmodel

import android.app.Application
import com.slowthemdown.android.data.datastore.CalibrationStore
import com.slowthemdown.android.data.db.SpeedEntryDao
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.android.debug.SeedData
import com.slowthemdown.shared.model.CalibrationMethod
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.TravelDirection
import com.slowthemdown.shared.model.VehicleType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dao: SpeedEntryDao
    private lateinit var calibrationStore: CalibrationStore
    private lateinit var application: Application

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = mockk(relaxed = true)
        calibrationStore = mockk(relaxed = true)
        application = mockk(relaxed = true)
        every { calibrationStore.measurementSystem } returns flowOf(MeasurementSystem.IMPERIAL)
        mockkObject(SeedData)
        every { SeedData.isSeeded(any()) } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(SeedData)
    }

    private fun makeEntry(
        id: String = "test-id",
        timestamp: Long = 1000L,
        speed: Double = 13.4,
        speedLimit: Double = 11.2,
        streetName: String = "",
        notes: String = "",
        vehicleType: VehicleType = VehicleType.CAR,
    ): SpeedEntryEntity = SpeedEntryEntity(
        id = id,
        timestamp = timestamp,
        speed = speed,
        speedLimit = speedLimit,
        streetName = streetName,
        notes = notes,
        vehicleTypeRaw = vehicleType.rawValue,
    )

    private fun createViewModel(entries: List<SpeedEntryEntity> = emptyList()): LogViewModel {
        every { dao.getAllEntries() } returns flowOf(entries)
        return LogViewModel(dao, calibrationStore, application)
    }

    // MARK: - Search filtering

    @Test
    fun emptySearch_returnsAll() = runTest {
        val entries = listOf(
            makeEntry(id = "1", streetName = "Main St"),
            makeEntry(id = "2", streetName = "Oak Ave"),
            makeEntry(id = "3", streetName = "Elm Rd"),
        )
        val vm = createViewModel(entries)
        assertEquals(3, vm.entries.first().size)
    }

    @Test
    fun searchByStreetName() = runTest {
        val entries = listOf(
            makeEntry(id = "1", streetName = "Main St"),
            makeEntry(id = "2", streetName = "Oak Ave"),
        )
        val vm = createViewModel(entries)
        vm.setSearchText("Main")
        val result = vm.entries.first()
        assertEquals(1, result.size)
        assertEquals("Main St", result[0].streetName)
    }

    @Test
    fun searchByNotes() = runTest {
        val entries = listOf(
            makeEntry(id = "1", notes = "speeding truck"),
            makeEntry(id = "2", notes = "school zone"),
        )
        val vm = createViewModel(entries)
        vm.setSearchText("school")
        val result = vm.entries.first()
        assertEquals(1, result.size)
    }

    @Test
    fun searchIsCaseInsensitive() = runTest {
        val entries = listOf(
            makeEntry(id = "1", streetName = "Main Street"),
            makeEntry(id = "2", streetName = "Oak Ave"),
        )
        val vm = createViewModel(entries)
        vm.setSearchText("MAIN")
        assertEquals(1, vm.entries.first().size)
    }

    // MARK: - Vehicle type filter

    @Test
    fun filterByVehicleType() = runTest {
        val entries = listOf(
            makeEntry(id = "1", vehicleType = VehicleType.CAR),
            makeEntry(id = "2", vehicleType = VehicleType.TRUCK),
            makeEntry(id = "3", vehicleType = VehicleType.CAR),
        )
        val vm = createViewModel(entries)
        vm.setVehicleTypeFilter(VehicleType.TRUCK)
        val result = vm.entries.first()
        assertEquals(1, result.size)
        assertEquals(VehicleType.TRUCK, result[0].vehicleType)
    }

    @Test
    fun nilVehicleTypeFilter_returnsAll() = runTest {
        val entries = listOf(
            makeEntry(id = "1", vehicleType = VehicleType.CAR),
            makeEntry(id = "2", vehicleType = VehicleType.TRUCK),
            makeEntry(id = "3", vehicleType = VehicleType.MOTORCYCLE),
        )
        val vm = createViewModel(entries)
        vm.setVehicleTypeFilter(null)
        assertEquals(3, vm.entries.first().size)
    }

    // MARK: - Over limit filter

    @Test
    fun overLimitOnly() = runTest {
        val entries = listOf(
            makeEntry(id = "1", speed = 15.0, speedLimit = 11.2),
            makeEntry(id = "2", speed = 8.0, speedLimit = 11.2),
            makeEntry(id = "3", speed = 20.0, speedLimit = 11.2),
        )
        val vm = createViewModel(entries)
        vm.setOverLimitOnly(true)
        val result = vm.entries.first()
        assertEquals(2, result.size)
        assertTrue(result.all { it.isOverLimit })
    }

    @Test
    fun overLimitFalse_returnsAll() = runTest {
        val entries = listOf(
            makeEntry(id = "1", speed = 15.0, speedLimit = 11.2),
            makeEntry(id = "2", speed = 8.0, speedLimit = 11.2),
        )
        val vm = createViewModel(entries)
        vm.setOverLimitOnly(false)
        assertEquals(2, vm.entries.first().size)
    }

    // MARK: - Sorting

    @Test
    fun sortNewestFirst() = runTest {
        val entries = listOf(
            makeEntry(id = "1", timestamp = 1000L),
            makeEntry(id = "2", timestamp = 3000L),
            makeEntry(id = "3", timestamp = 2000L),
        )
        val vm = createViewModel(entries)
        val result = vm.entries.first()
        assertEquals("2", result[0].id)
        assertEquals("3", result[1].id)
        assertEquals("1", result[2].id)
    }

    @Test
    fun sortOldestFirst() = runTest {
        val entries = listOf(
            makeEntry(id = "1", timestamp = 1000L),
            makeEntry(id = "2", timestamp = 3000L),
            makeEntry(id = "3", timestamp = 2000L),
        )
        val vm = createViewModel(entries)
        vm.toggleSortOrder()
        assertEquals(SortOrder.OLDEST_FIRST, vm.sortOrder.value)
        val result = vm.entries.first()
        assertEquals("1", result[0].id)
        assertEquals("3", result[1].id)
        assertEquals("2", result[2].id)
    }

    // MARK: - Combined filters

    @Test
    fun combinedFilters() = runTest {
        val entries = listOf(
            makeEntry(id = "1", streetName = "Main St", speed = 15.0, speedLimit = 11.2, vehicleType = VehicleType.CAR, timestamp = 1000L),
            makeEntry(id = "2", streetName = "Main Ave", speed = 8.0, speedLimit = 11.2, vehicleType = VehicleType.CAR, timestamp = 2000L),
            makeEntry(id = "3", streetName = "Oak Rd", speed = 20.0, speedLimit = 11.2, vehicleType = VehicleType.TRUCK, timestamp = 3000L),
            makeEntry(id = "4", streetName = "Main Blvd", speed = 18.0, speedLimit = 11.2, vehicleType = VehicleType.CAR, timestamp = 4000L),
        )
        val vm = createViewModel(entries)
        vm.setSearchText("Main")
        vm.setOverLimitOnly(true)
        vm.setVehicleTypeFilter(VehicleType.CAR)
        val result = vm.entries.first()
        assertEquals(2, result.size)
        assertEquals("4", result[0].id)
        assertEquals("1", result[1].id)
    }

    // MARK: - Edge cases

    @Test
    fun emptyArray_returnsEmpty() = runTest {
        val vm = createViewModel(emptyList())
        assertTrue(vm.entries.first().isEmpty())
    }

    @Test
    fun delete_callsDaoDelete() {
        val entry = makeEntry(id = "1")
        val vm = createViewModel(listOf(entry))
        vm.delete(entry)
        coVerify { dao.delete(entry) }
    }
}
