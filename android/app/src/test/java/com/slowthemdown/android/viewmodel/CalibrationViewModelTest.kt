package com.slowthemdown.android.viewmodel

import android.graphics.Bitmap
import com.slowthemdown.android.data.datastore.Calibration
import com.slowthemdown.android.data.datastore.CalibrationStore
import com.slowthemdown.android.service.HapticManager
import com.slowthemdown.android.service.PIIBlurService
import com.slowthemdown.shared.calculator.CoordinateMapper
import com.slowthemdown.shared.calculator.Point
import com.slowthemdown.shared.calculator.Size
import com.slowthemdown.shared.model.CalibrationMethod
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.VehicleReferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalibrationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val calibrationStore = mockk<CalibrationStore>()
    private val hapticManager = mockk<HapticManager>(relaxed = true)
    private val piiBlurService = mockk<PIIBlurService>(relaxed = true)

    private val mockBitmap = mockk<Bitmap>()

    private lateinit var vm: CalibrationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { calibrationStore.calibration } returns flowOf(Calibration())
        every { calibrationStore.measurementSystem } returns flowOf(MeasurementSystem.IMPERIAL)
        coEvery { calibrationStore.save(any()) } returns Unit
        coEvery { calibrationStore.saveMeasurementSystem(any()) } returns Unit
        every { mockBitmap.width } returns 1920
        every { mockBitmap.height } returns 1080
        coEvery { piiBlurService.blurPII(any()) } returns mockBitmap
        vm = CalibrationViewModel(calibrationStore, hapticManager, piiBlurService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // MARK: - Initial state

    @Test
    fun initialState_notCalibrated() = runTest {
        assertFalse(vm.calibration.first().isValid)
    }

    @Test
    fun initialState_emptyMarkers() {
        assertTrue(vm.markers.value.isEmpty())
    }

    // MARK: - Pixel distance

    @Test
    fun pixelDistance_withNoMarkers_returnsZero() = runTest {
        assertEquals(0.0, vm.pixelDistance.first(), 0.001)
    }

    @Test
    fun pixelDistance_withTwoMarkers_returnsDistance() = runTest {
        vm.setImage(mockBitmap)

        val viewSize = Size(1920.0, 1080.0)
        vm.addMarker(Point(100.0, 200.0), viewSize)
        vm.addMarker(Point(400.0, 600.0), viewSize)

        val markers = vm.markers.value
        assertEquals(2, markers.size)
        val expected = CoordinateMapper.pixelDistance(markers[0], markers[1])
        assertEquals(expected, vm.pixelDistance.first(), 0.001)
    }

    // MARK: - canSave

    @Test
    fun canSave_requiresMarkersAndDistance() = runTest {
        vm.setImage(mockBitmap)

        // No markers, no distance
        assertFalse(vm.canSave.first())

        // Two markers but no distance
        val viewSize = Size(1920.0, 1080.0)
        vm.addMarker(Point(100.0, 200.0), viewSize)
        vm.addMarker(Point(400.0, 600.0), viewSize)
        assertFalse(vm.canSave.first())

        // Two markers and distance
        vm.setReferenceDistance(5.0)
        assertTrue(vm.canSave.first())
    }

    @Test
    fun canSave_zeroDistance_isFalse() = runTest {
        vm.setImage(mockBitmap)

        val viewSize = Size(1920.0, 1080.0)
        vm.addMarker(Point(100.0, 200.0), viewSize)
        vm.addMarker(Point(400.0, 600.0), viewSize)
        vm.setReferenceDistance(0.0)

        assertFalse(vm.canSave.first())
    }

    // MARK: - Markers

    @Test
    fun addMarker_appendsUpToTwo() = runTest {
        vm.setImage(mockBitmap)

        val viewSize = Size(1920.0, 1080.0)
        vm.addMarker(Point(10.0, 10.0), viewSize)
        assertEquals(1, vm.markers.value.size)

        vm.addMarker(Point(20.0, 20.0), viewSize)
        assertEquals(2, vm.markers.value.size)
    }

    @Test
    fun addMarker_replacesWhenAlreadyTwo() = runTest {
        vm.setImage(mockBitmap)

        val viewSize = Size(1920.0, 1080.0)
        vm.addMarker(Point(10.0, 10.0), viewSize)
        vm.addMarker(Point(20.0, 20.0), viewSize)
        assertEquals(2, vm.markers.value.size)

        vm.addMarker(Point(30.0, 30.0), viewSize)
        assertEquals(1, vm.markers.value.size)
    }

    @Test
    fun resetMarkers_clearsAll() = runTest {
        vm.setImage(mockBitmap)

        val viewSize = Size(1920.0, 1080.0)
        vm.addMarker(Point(10.0, 10.0), viewSize)
        vm.addMarker(Point(20.0, 20.0), viewSize)
        assertEquals(2, vm.markers.value.size)

        vm.resetMarkers()
        assertTrue(vm.markers.value.isEmpty())
    }

    // MARK: - Setters

    @Test
    fun setMethod_updatesState() {
        vm.setMethod(CalibrationMethod.VEHICLE_REFERENCE)
        assertEquals(CalibrationMethod.VEHICLE_REFERENCE, vm.method.value)
    }

    @Test
    fun setReferenceDistance_updatesState() {
        vm.setReferenceDistance(6.096)
        assertEquals(6.096, vm.referenceDistance.value, 0.001)
    }

    @Test
    fun setSelectedVehicleRef_updatesState() {
        val ref = VehicleReferences.all.first()
        vm.setSelectedVehicleRef(ref)
        assertEquals(ref, vm.selectedVehicleRef.value)
    }

    @Test
    fun setSelectedVehicleRef_null_clearsState() {
        vm.setSelectedVehicleRef(VehicleReferences.all.first())
        vm.setSelectedVehicleRef(null)
        assertNull(vm.selectedVehicleRef.value)
    }

    // MARK: - Save calibration

    @Test
    fun saveCalibration_callsStoreWithCorrectValues() = runTest {
        vm.setImage(mockBitmap)

        val viewSize = Size(1920.0, 1080.0)
        vm.addMarker(Point(100.0, 200.0), viewSize)
        vm.addMarker(Point(400.0, 600.0), viewSize)
        vm.setReferenceDistance(6.096)

        vm.saveCalibration()

        coVerify {
            calibrationStore.save(match { calibration ->
                calibration.pixelsPerMeter > 0 &&
                    calibration.referenceDistanceMeters == 6.096 &&
                    calibration.method == CalibrationMethod.MANUAL_DISTANCE &&
                    calibration.calibrationImageWidth == 1920.0
            })
        }
    }

    @Test
    fun saveCalibration_withVehicleReference_usesRefLength() = runTest {
        vm.setImage(mockBitmap)

        val ref = VehicleReferences.all.first()
        vm.setMethod(CalibrationMethod.VEHICLE_REFERENCE)
        vm.setSelectedVehicleRef(ref)

        val viewSize = Size(1920.0, 1080.0)
        vm.addMarker(Point(100.0, 200.0), viewSize)
        vm.addMarker(Point(400.0, 600.0), viewSize)

        vm.saveCalibration()

        coVerify {
            calibrationStore.save(match { calibration ->
                calibration.referenceDistanceMeters == ref.lengthMeters &&
                    calibration.method == CalibrationMethod.VEHICLE_REFERENCE &&
                    calibration.vehicleReferenceName == ref.name
            })
        }
    }

    // MARK: - Clear calibration

    @Test
    fun clearCalibration_resetsEverything() = runTest {
        vm.setImage(mockBitmap)

        val viewSize = Size(1920.0, 1080.0)
        vm.addMarker(Point(100.0, 200.0), viewSize)
        vm.addMarker(Point(400.0, 600.0), viewSize)
        vm.setReferenceDistance(6.096)

        vm.clearCalibration()

        coVerify {
            calibrationStore.save(match { it.pixelsPerMeter == 0.0 && !it.isValid })
        }
        assertNull(vm.selectedImageBitmap.value)
        assertEquals(Size.ZERO, vm.imageSize.value)
        assertTrue(vm.markers.value.isEmpty())
        assertEquals(0.0, vm.referenceDistance.value, 0.001)
    }
}
