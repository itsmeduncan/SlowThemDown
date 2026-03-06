package com.slowthemdown.android.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowthemdown.android.data.datastore.Calibration
import com.slowthemdown.android.data.datastore.CalibrationStore
import com.slowthemdown.android.data.db.SpeedEntryDao
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.android.service.LocationService
import com.slowthemdown.android.service.VideoFrameExtractor
import com.slowthemdown.shared.calculator.CoordinateMapper
import com.slowthemdown.shared.calculator.Point
import com.slowthemdown.shared.calculator.Size
import com.slowthemdown.shared.calculator.SpeedCalculator
import com.slowthemdown.shared.model.CalibrationMethod
import com.slowthemdown.shared.model.RoadStandards
import com.slowthemdown.shared.model.TravelDirection
import com.slowthemdown.shared.model.VehicleReference
import com.slowthemdown.shared.model.VehicleType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min

enum class CaptureFlowState {
    SELECT_SOURCE,
    RECORDING,
    SELECT_FRAMES,
    MARK_FRAME1,
    MARK_FRAME2,
    RESULT,
}

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val frameExtractor: VideoFrameExtractor,
    private val locationService: LocationService,
    private val calibrationStore: CalibrationStore,
    private val speedEntryDao: SpeedEntryDao,
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureFlowState.SELECT_SOURCE)
    val state: StateFlow<CaptureFlowState> = _state.asStateFlow()

    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri.asStateFlow()

    private val _videoDurationSeconds = MutableStateFlow(0.0)
    val videoDurationSeconds: StateFlow<Double> = _videoDurationSeconds.asStateFlow()

    private val _videoWidth = MutableStateFlow(0)
    private val _videoHeight = MutableStateFlow(0)

    private val _frame1Time = MutableStateFlow(0.0)
    val frame1Time: StateFlow<Double> = _frame1Time.asStateFlow()

    private val _frame2Time = MutableStateFlow(0.5)
    val frame2Time: StateFlow<Double> = _frame2Time.asStateFlow()

    private val _frame1Image = MutableStateFlow<Bitmap?>(null)
    val frame1Image: StateFlow<Bitmap?> = _frame1Image.asStateFlow()

    private val _frame2Image = MutableStateFlow<Bitmap?>(null)
    val frame2Image: StateFlow<Bitmap?> = _frame2Image.asStateFlow()

    private val _frame1Marker = MutableStateFlow<Point?>(null)
    val frame1Marker: StateFlow<Point?> = _frame1Marker.asStateFlow()

    private val _frame2Marker = MutableStateFlow<Point?>(null)
    val frame2Marker: StateFlow<Point?> = _frame2Marker.asStateFlow()

    private val _useVehicleReference = MutableStateFlow(false)
    val useVehicleReference: StateFlow<Boolean> = _useVehicleReference.asStateFlow()

    private val _selectedVehicleRef = MutableStateFlow<VehicleReference?>(null)
    val selectedVehicleRef: StateFlow<VehicleReference?> = _selectedVehicleRef.asStateFlow()

    private val _vehicleRefMarkers = MutableStateFlow<List<Point>>(emptyList())
    val vehicleRefMarkers: StateFlow<List<Point>> = _vehicleRefMarkers.asStateFlow()

    private val _calculatedSpeed = MutableStateFlow(0.0)
    val calculatedSpeed: StateFlow<Double> = _calculatedSpeed.asStateFlow()

    private val _vehicleType = MutableStateFlow(VehicleType.CAR)
    val vehicleType: StateFlow<VehicleType> = _vehicleType.asStateFlow()

    private val _direction = MutableStateFlow(TravelDirection.LEFT_TO_RIGHT)
    val direction: StateFlow<TravelDirection> = _direction.asStateFlow()

    private val _speedLimit = MutableStateFlow(RoadStandards.defaultSpeedLimit)
    val speedLimit: StateFlow<Int> = _speedLimit.asStateFlow()

    private val _streetName = MutableStateFlow("")
    val streetName: StateFlow<String> = _streetName.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    val calibration: StateFlow<Calibration> = calibrationStore.calibration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Calibration())

    private val videoSize: Size
        get() = Size(_videoWidth.value.toDouble(), _videoHeight.value.toDouble())

    private val timeDelta: Double
        get() = abs(_frame2Time.value - _frame1Time.value)

    private val pixelDisplacement: Double
        get() {
            val m1 = _frame1Marker.value ?: return 0.0
            val m2 = _frame2Marker.value ?: return 0.0
            return CoordinateMapper.pixelDistance(m1, m2)
        }

    fun loadVideo(uri: Uri) {
        _videoUri.value = uri
        viewModelScope.launch {
            try {
                val info = frameExtractor.getVideoInfo(uri)
                _videoDurationSeconds.value = info.durationMs / 1000.0
                _videoWidth.value = info.width
                _videoHeight.value = info.height
                _frame1Time.value = 0.0
                _frame2Time.value = min(0.5, _videoDurationSeconds.value)
                _state.value = CaptureFlowState.SELECT_FRAMES
            } catch (_: Exception) {
                _state.value = CaptureFlowState.SELECT_SOURCE
            }
        }
    }

    fun setFrame1Time(time: Double) { _frame1Time.value = time }
    fun setFrame2Time(time: Double) { _frame2Time.value = time }

    fun extractFrames() {
        val uri = _videoUri.value ?: return
        viewModelScope.launch {
            try {
                _frame1Image.value = frameExtractor.extractFrame(uri, _frame1Time.value)
                _frame2Image.value = frameExtractor.extractFrame(uri, _frame2Time.value)
                _frame1Marker.value = null
                _frame2Marker.value = null
                _vehicleRefMarkers.value = emptyList()
                _state.value = CaptureFlowState.MARK_FRAME1
            } catch (_: Exception) { /* stay on frame selection */ }
        }
    }

    fun addMarkerFrame1(viewPoint: Point, viewSize: Size) {
        _frame1Marker.value = CoordinateMapper.viewToImage(viewPoint, viewSize, videoSize)
    }

    fun addMarkerFrame2(viewPoint: Point, viewSize: Size) {
        _frame2Marker.value = CoordinateMapper.viewToImage(viewPoint, viewSize, videoSize)
    }

    fun advanceToMarkFrame2() { _state.value = CaptureFlowState.MARK_FRAME2 }

    fun setUseVehicleReference(use: Boolean) { _useVehicleReference.value = use }
    fun setSelectedVehicleRef(ref: VehicleReference?) { _selectedVehicleRef.value = ref }
    fun setVehicleType(type: VehicleType) { _vehicleType.value = type }
    fun setDirection(dir: TravelDirection) { _direction.value = dir }
    fun setSpeedLimit(limit: Int) { _speedLimit.value = limit }
    fun setStreetName(name: String) { _streetName.value = name }
    fun setNotes(notes: String) { _notes.value = notes }

    fun addVehicleRefMarker(viewPoint: Point, viewSize: Size) {
        val imagePoint = CoordinateMapper.viewToImage(viewPoint, viewSize, videoSize)
        val current = _vehicleRefMarkers.value
        _vehicleRefMarkers.value = if (current.size >= 2) listOf(imagePoint) else current + imagePoint
    }

    fun calculateSpeed() {
        val cal = calibration.value
        val ppf: Double = if (_useVehicleReference.value) {
            val ref = _selectedVehicleRef.value ?: return
            val markers = _vehicleRefMarkers.value
            if (markers.size != 2) return
            val refPixels = CoordinateMapper.pixelDistance(markers[0], markers[1])
            SpeedCalculator.pixelsPerFoot(refPixels, ref.lengthFeet)
        } else {
            cal.pixelsPerFoot
        }

        _calculatedSpeed.value = SpeedCalculator.calculateSpeedMPH(
            pixelDisplacement = pixelDisplacement,
            pixelsPerFoot = ppf,
            timeDeltaSeconds = timeDelta
        )
        _state.value = CaptureFlowState.RESULT
    }

    fun saveEntry() {
        viewModelScope.launch {
            val cal = calibration.value
            val location = locationService.getCurrentLocation()
            val street = if (_streetName.value.isEmpty() && location != null) {
                locationService.getStreetName(location)
            } else {
                _streetName.value
            }

            val ppf: Double
            val method: CalibrationMethod
            val refDist: Double
            if (_useVehicleReference.value && _selectedVehicleRef.value != null && _vehicleRefMarkers.value.size == 2) {
                val ref = _selectedVehicleRef.value!!
                val markers = _vehicleRefMarkers.value
                val refPixels = CoordinateMapper.pixelDistance(markers[0], markers[1])
                ppf = SpeedCalculator.pixelsPerFoot(refPixels, ref.lengthFeet)
                method = CalibrationMethod.VEHICLE_REFERENCE
                refDist = ref.lengthFeet
            } else {
                ppf = cal.pixelsPerFoot
                method = cal.method
                refDist = cal.referenceDistanceFeet
            }

            val entry = SpeedEntryEntity(
                speedMPH = _calculatedSpeed.value,
                speedLimit = _speedLimit.value,
                streetName = street,
                notes = _notes.value,
                vehicleTypeRaw = _vehicleType.value.rawValue,
                directionRaw = _direction.value.rawValue,
                calibrationMethodRaw = method.rawValue,
                timeDeltaSeconds = timeDelta,
                pixelDisplacement = pixelDisplacement,
                pixelsPerFoot = ppf,
                referenceDistanceFeet = refDist,
                latitude = location?.latitude,
                longitude = location?.longitude,
            )
            speedEntryDao.insert(entry)
            reset()
        }
    }

    fun reset() {
        _state.value = CaptureFlowState.SELECT_SOURCE
        _videoUri.value = null
        _frame1Image.value = null
        _frame2Image.value = null
        _frame1Marker.value = null
        _frame2Marker.value = null
        _vehicleRefMarkers.value = emptyList()
        _calculatedSpeed.value = 0.0
        _notes.value = ""
        _streetName.value = ""
        _useVehicleReference.value = false
        _selectedVehicleRef.value = null
    }
}
