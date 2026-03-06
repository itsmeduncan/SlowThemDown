package com.slowthemdown.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowthemdown.android.data.datastore.Calibration
import com.slowthemdown.android.data.datastore.CalibrationStore
import com.slowthemdown.shared.calculator.CoordinateMapper
import com.slowthemdown.shared.calculator.Point
import com.slowthemdown.shared.calculator.Size
import com.slowthemdown.shared.calculator.SpeedCalculator
import com.slowthemdown.shared.model.CalibrationMethod
import com.slowthemdown.shared.model.VehicleReference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationStore: CalibrationStore,
) : ViewModel() {

    val calibration: StateFlow<Calibration> = calibrationStore.calibration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Calibration())

    private val _method = MutableStateFlow(CalibrationMethod.MANUAL_DISTANCE)
    val method: StateFlow<CalibrationMethod> = _method.asStateFlow()

    private val _referenceDistanceFeet = MutableStateFlow(0.0)
    val referenceDistanceFeet: StateFlow<Double> = _referenceDistanceFeet.asStateFlow()

    private val _markers = MutableStateFlow<List<Point>>(emptyList())
    val markers: StateFlow<List<Point>> = _markers.asStateFlow()

    private val _selectedVehicleRef = MutableStateFlow<VehicleReference?>(null)
    val selectedVehicleRef: StateFlow<VehicleReference?> = _selectedVehicleRef.asStateFlow()

    fun setMethod(method: CalibrationMethod) { _method.value = method }
    fun setReferenceDistance(feet: Double) { _referenceDistanceFeet.value = feet }
    fun setSelectedVehicleRef(ref: VehicleReference?) { _selectedVehicleRef.value = ref }

    fun addMarker(viewPoint: Point, viewSize: Size, imageSize: Size) {
        val imagePoint = CoordinateMapper.viewToImage(viewPoint, viewSize, imageSize)
        val current = _markers.value
        _markers.value = if (current.size >= 2) listOf(imagePoint) else current + imagePoint
    }

    fun saveCalibration() {
        val markers = _markers.value
        if (markers.size != 2) return

        val pixelDist = CoordinateMapper.pixelDistance(markers[0], markers[1])
        val refFeet = when (_method.value) {
            CalibrationMethod.MANUAL_DISTANCE -> _referenceDistanceFeet.value
            CalibrationMethod.VEHICLE_REFERENCE -> _selectedVehicleRef.value?.lengthFeet ?: return
        }
        val ppf = SpeedCalculator.pixelsPerFoot(pixelDist, refFeet)
        if (ppf <= 0) return

        viewModelScope.launch {
            calibrationStore.save(
                Calibration(
                    method = _method.value,
                    pixelsPerFoot = ppf,
                    referenceDistanceFeet = refFeet,
                    pixelDistance = pixelDist,
                    vehicleReferenceName = _selectedVehicleRef.value?.name,
                )
            )
        }
    }
}
