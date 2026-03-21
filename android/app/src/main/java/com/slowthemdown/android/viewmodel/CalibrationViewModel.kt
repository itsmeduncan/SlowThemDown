package com.slowthemdown.android.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowthemdown.android.data.datastore.Calibration
import com.slowthemdown.android.data.datastore.CalibrationStore
import com.slowthemdown.android.service.HapticManager
import com.slowthemdown.android.service.PIIBlurService
import com.slowthemdown.shared.calculator.CoordinateMapper
import com.slowthemdown.shared.calculator.Point
import com.slowthemdown.shared.calculator.Size
import com.slowthemdown.shared.calculator.SpeedCalculator
import com.slowthemdown.shared.model.CalibrationMethod
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.VehicleReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationStore: CalibrationStore,
    private val hapticManager: HapticManager,
    private val piiBlurService: PIIBlurService,
) : ViewModel() {

    val calibration: StateFlow<Calibration> = calibrationStore.calibration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Calibration())

    val measurementSystem: StateFlow<MeasurementSystem> = calibrationStore.measurementSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MeasurementSystem.IMPERIAL)

    private val _method = MutableStateFlow(CalibrationMethod.MANUAL_DISTANCE)
    val method: StateFlow<CalibrationMethod> = _method.asStateFlow()

    /** Reference distance in meters (always stored as meters internally) */
    private val _referenceDistance = MutableStateFlow(0.0)
    val referenceDistance: StateFlow<Double> = _referenceDistance.asStateFlow()

    private val _markers = MutableStateFlow<List<Point>>(emptyList())
    val markers: StateFlow<List<Point>> = _markers.asStateFlow()

    private val _selectedVehicleRef = MutableStateFlow<VehicleReference?>(null)
    val selectedVehicleRef: StateFlow<VehicleReference?> = _selectedVehicleRef.asStateFlow()

    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    private val _imageSize = MutableStateFlow(Size.ZERO)
    val imageSize: StateFlow<Size> = _imageSize.asStateFlow()

    val pixelDistance: StateFlow<Double> = _markers.combine(_markers) { markers, _ ->
        if (markers.size == 2) CoordinateMapper.pixelDistance(markers[0], markers[1]) else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val canSave: StateFlow<Boolean> = combine(_markers, _referenceDistance) { markers, dist ->
        markers.size == 2 && dist > 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setMethod(method: CalibrationMethod) { _method.value = method }

    /** Set reference distance in meters */
    fun setReferenceDistance(meters: Double) { _referenceDistance.value = meters }

    fun setSelectedVehicleRef(ref: VehicleReference?) { _selectedVehicleRef.value = ref }

    fun setMeasurementSystem(system: MeasurementSystem) {
        viewModelScope.launch {
            calibrationStore.saveMeasurementSystem(system)
        }
    }

    fun setImage(bitmap: Bitmap) {
        viewModelScope.launch {
            val blurred = piiBlurService.blurPII(bitmap)
            _selectedImageBitmap.value = blurred
            _imageSize.value = Size(blurred.width.toDouble(), blurred.height.toDouble())
            resetMarkers()
        }
    }

    fun clearImage() {
        _selectedImageBitmap.value = null
        _imageSize.value = Size.ZERO
        resetMarkers()
    }

    fun resetMarkers() {
        _markers.value = emptyList()
    }

    fun addMarker(viewPoint: Point, viewSize: Size) {
        val imagePoint = CoordinateMapper.viewToImage(viewPoint, viewSize, _imageSize.value)
        val current = _markers.value
        _markers.value = if (current.size >= 2) listOf(imagePoint) else current + imagePoint
        hapticManager.impact(HapticManager.ImpactStyle.MEDIUM)
    }

    fun saveCalibration() {
        val markers = _markers.value
        if (markers.size != 2) return

        val pixelDist = CoordinateMapper.pixelDistance(markers[0], markers[1])
        val refMeters = when (_method.value) {
            CalibrationMethod.MANUAL_DISTANCE -> _referenceDistance.value
            CalibrationMethod.VEHICLE_REFERENCE -> _selectedVehicleRef.value?.lengthMeters ?: return
        }
        val ppm = SpeedCalculator.pixelsPerMeter(pixelDist, refMeters)
        if (ppm <= 0) return

        viewModelScope.launch {
            calibrationStore.save(
                Calibration(
                    method = _method.value,
                    pixelsPerMeter = ppm,
                    referenceDistanceMeters = refMeters,
                    pixelDistance = pixelDist,
                    vehicleReferenceName = _selectedVehicleRef.value?.name,
                )
            )
            hapticManager.notification(HapticManager.NotificationType.SUCCESS)
        }
    }

    fun clearCalibration() {
        viewModelScope.launch {
            calibrationStore.save(Calibration())
        }
        clearImage()
        _referenceDistance.value = 0.0
    }
}
