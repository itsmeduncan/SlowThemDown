package com.slowthemdown.android.viewmodel

import com.slowthemdown.android.data.datastore.Calibration
import com.slowthemdown.android.data.datastore.CalibrationStore
import com.slowthemdown.android.data.db.SpeedEntryDao
import com.slowthemdown.android.service.HapticManager
import com.slowthemdown.android.service.LocationService
import com.slowthemdown.android.service.PIIBlurService
import com.slowthemdown.android.service.VideoFrameExtractor
import com.slowthemdown.shared.calculator.CoordinateMapper
import com.slowthemdown.shared.calculator.Point
import com.slowthemdown.shared.calculator.Size
import com.slowthemdown.shared.model.CalibrationMethod
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.RoadStandards
import com.slowthemdown.shared.model.TravelDirection
import com.slowthemdown.shared.model.VehicleReferences
import com.slowthemdown.shared.model.VehicleType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val frameExtractor = mockk<VideoFrameExtractor>(relaxed = true)
    private val locationService = mockk<LocationService>(relaxed = true)
    private val calibrationStore = mockk<CalibrationStore>()
    private val speedEntryDao = mockk<SpeedEntryDao>(relaxed = true)
    private val hapticManager = mockk<HapticManager>(relaxed = true)
    private val piiBlurService = mockk<PIIBlurService>(relaxed = true)

    private lateinit var vm: CaptureViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { calibrationStore.calibration } returns flowOf(
            Calibration(pixelsPerMeter = 32.8084, referenceDistanceMeters = 6.096)
        )
        every { calibrationStore.measurementSystem } returns flowOf(MeasurementSystem.IMPERIAL)
        vm = CaptureViewModel(frameExtractor, locationService, calibrationStore, speedEntryDao, hapticManager, piiBlurService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // MARK: - Initial state

    @Test
    fun initialState_isSelectSource() {
        assertEquals(CaptureFlowState.SELECT_SOURCE, vm.state.value)
    }

    @Test
    fun initialState_fieldsAreDefaults() {
        assertNull(vm.videoUri.value)
        assertEquals(0.0, vm.calculatedSpeed.value, 0.001)
        assertEquals("", vm.streetName.value)
        assertEquals("", vm.notes.value)
        assertEquals(VehicleType.CAR, vm.vehicleType.value)
        assertEquals(TravelDirection.LEFT_TO_RIGHT, vm.direction.value)
    }

    // MARK: - Setters

    @Test
    fun setVehicleType_updatesState() {
        vm.setVehicleType(VehicleType.TRUCK)
        assertEquals(VehicleType.TRUCK, vm.vehicleType.value)
    }

    @Test
    fun setDirection_updatesState() {
        vm.setDirection(TravelDirection.TOWARD)
        assertEquals(TravelDirection.TOWARD, vm.direction.value)
    }

    @Test
    fun setSpeedLimit_updatesState() {
        val limit = RoadStandards.imperialSpeedLimits[3] // 30 MPH in m/s
        vm.setSpeedLimit(limit)
        assertEquals(limit, vm.speedLimit.value, 0.001)
    }

    @Test
    fun setStreetName_updatesState() {
        vm.setStreetName("Main St")
        assertEquals("Main St", vm.streetName.value)
    }

    @Test
    fun setNotes_updatesState() {
        vm.setNotes("Red sedan")
        assertEquals("Red sedan", vm.notes.value)
    }

    // MARK: - State transitions

    @Test
    fun advanceToMarkFrame2_transitionsState() {
        vm.advanceToMarkFrame2()
        assertEquals(CaptureFlowState.MARK_FRAME2, vm.state.value)
    }

    // MARK: - Markers

    @Test
    fun addMarkerFrame1_setsMarker() {
        val viewPoint = Point(100.0, 200.0)
        val viewSize = Size(400.0, 800.0)
        vm.addMarkerFrame1(viewPoint, viewSize)
        // Marker is set (coordinates depend on video size, which is 0x0 here)
        assertTrue(vm.frame1Marker.value != null)
    }

    @Test
    fun addVehicleRefMarker_appendsUpToTwo() {
        val viewSize = Size(400.0, 800.0)
        vm.addVehicleRefMarker(Point(10.0, 10.0), viewSize)
        assertEquals(1, vm.vehicleRefMarkers.value.size)

        vm.addVehicleRefMarker(Point(20.0, 20.0), viewSize)
        assertEquals(2, vm.vehicleRefMarkers.value.size)
    }

    @Test
    fun addVehicleRefMarker_replacesWhenAlreadyTwo() {
        val viewSize = Size(400.0, 800.0)
        vm.addVehicleRefMarker(Point(10.0, 10.0), viewSize)
        vm.addVehicleRefMarker(Point(20.0, 20.0), viewSize)
        assertEquals(2, vm.vehicleRefMarkers.value.size)

        vm.addVehicleRefMarker(Point(30.0, 30.0), viewSize)
        assertEquals(1, vm.vehicleRefMarkers.value.size)
    }

    // MARK: - Computed properties

    @Test
    fun timeDelta_returnsAbsoluteDifference() {
        vm.setFrame1Time(1.0)
        vm.setFrame2Time(1.5)
        assertTrue(kotlin.math.abs(vm.timeDelta - 0.5) < 0.001)
    }

    @Test
    fun timeDelta_handlesReversedOrder() {
        vm.setFrame1Time(2.0)
        vm.setFrame2Time(1.0)
        assertTrue(kotlin.math.abs(vm.timeDelta - 1.0) < 0.001)
    }

    @Test
    fun pixelDisplacement_withNoMarkers_returnsZero() {
        assertEquals(0.0, vm.pixelDisplacement, 0.001)
    }

    // MARK: - calculateSpeed

    @Test
    fun calculateSpeed_withoutVehicleRef_usesCalibration() {
        // Set up markers so we have pixel displacement
        vm.setFrame1Time(0.0)
        vm.setFrame2Time(1.0)
        vm.setUseVehicleReference(false)

        vm.calculateSpeed()

        assertEquals(CaptureFlowState.RESULT, vm.state.value)
    }

    @Test
    fun calculateSpeed_withVehicleRef_missingRef_doesNotTransition() {
        vm.setUseVehicleReference(true)
        // No vehicle ref selected
        vm.calculateSpeed()
        // Should NOT transition to RESULT because ref is null
        assertEquals(CaptureFlowState.SELECT_SOURCE, vm.state.value)
    }

    @Test
    fun calculateSpeed_withVehicleRef_insufficientMarkers_doesNotTransition() {
        vm.setUseVehicleReference(true)
        vm.setSelectedVehicleRef(VehicleReferences.all.first())
        // Only one marker
        vm.addVehicleRefMarker(Point(10.0, 10.0), Size(400.0, 800.0))
        vm.calculateSpeed()
        assertEquals(CaptureFlowState.SELECT_SOURCE, vm.state.value)
    }

    // MARK: - Reset

    @Test
    fun reset_restoresDefaults() {
        vm.setStreetName("Elm St")
        vm.setNotes("Test")
        vm.setVehicleType(VehicleType.BUS)
        vm.setUseVehicleReference(true)
        vm.setSelectedVehicleRef(VehicleReferences.all.first())

        vm.reset()

        assertEquals(CaptureFlowState.SELECT_SOURCE, vm.state.value)
        assertNull(vm.videoUri.value)
        assertEquals("", vm.streetName.value)
        assertEquals("", vm.notes.value)
        assertEquals(0.0, vm.calculatedSpeed.value, 0.001)
        assertNull(vm.frame1Marker.value)
        assertNull(vm.frame2Marker.value)
        assertTrue(vm.vehicleRefMarkers.value.isEmpty())
        assertEquals(false, vm.useVehicleReference.value)
        assertNull(vm.selectedVehicleRef.value)
    }
}
