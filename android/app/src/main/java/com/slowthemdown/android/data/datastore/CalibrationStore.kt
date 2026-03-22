package com.slowthemdown.android.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.slowthemdown.shared.model.CalibrationMethod
import com.slowthemdown.shared.model.MeasurementSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Calibration(
    val method: CalibrationMethod = CalibrationMethod.MANUAL_DISTANCE,
    val pixelsPerMeter: Double = 0.0,
    val referenceDistanceMeters: Double = 0.0,
    val pixelDistance: Double = 0.0,
    val vehicleReferenceName: String? = null,
    val calibrationImageWidth: Double? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    val isValid: Boolean get() = pixelsPerMeter > 0
    val needsRecalibration: Boolean get() = isValid && calibrationImageWidth == null

    fun scaledPixelsPerMeter(videoWidth: Double): Double {
        val calWidth = calibrationImageWidth ?: return pixelsPerMeter
        if (calWidth <= 0) return pixelsPerMeter
        return pixelsPerMeter * (videoWidth / calWidth)
    }
}

private val Context.dataStore by preferencesDataStore(name = "calibration")

@Singleton
class CalibrationStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val METHOD = stringPreferencesKey("method")
        val PIXELS_PER_METER = doublePreferencesKey("pixels_per_meter")
        val REFERENCE_DISTANCE_METERS = doublePreferencesKey("reference_distance_meters")
        val PIXEL_DISTANCE = doublePreferencesKey("pixel_distance")
        val VEHICLE_REF_NAME = stringPreferencesKey("vehicle_ref_name")
        val TIMESTAMP = longPreferencesKey("timestamp")
        val CALIBRATION_IMAGE_WIDTH = doublePreferencesKey("calibration_image_width")
        val MEASUREMENT_SYSTEM = stringPreferencesKey("measurement_system")

        // Legacy keys for migration
        val LEGACY_PIXELS_PER_FOOT = doublePreferencesKey("pixels_per_foot")
        val LEGACY_REFERENCE_DISTANCE = doublePreferencesKey("reference_distance")
    }

    val calibration: Flow<Calibration> = context.dataStore.data.map { prefs ->
        // Try new keys first, fall back to converted legacy values
        val ppm = prefs[Keys.PIXELS_PER_METER]
            ?: prefs[Keys.LEGACY_PIXELS_PER_FOOT]?.let { it * 3.28084 }
            ?: 0.0
        val refMeters = prefs[Keys.REFERENCE_DISTANCE_METERS]
            ?: prefs[Keys.LEGACY_REFERENCE_DISTANCE]?.let { it * 0.3048 }
            ?: 0.0

        Calibration(
            method = prefs[Keys.METHOD]?.let { CalibrationMethod.fromRawValue(it) }
                ?: CalibrationMethod.MANUAL_DISTANCE,
            pixelsPerMeter = ppm,
            referenceDistanceMeters = refMeters,
            pixelDistance = prefs[Keys.PIXEL_DISTANCE] ?: 0.0,
            vehicleReferenceName = prefs[Keys.VEHICLE_REF_NAME],
            calibrationImageWidth = prefs[Keys.CALIBRATION_IMAGE_WIDTH],
            timestampMillis = prefs[Keys.TIMESTAMP] ?: System.currentTimeMillis(),
        )
    }

    suspend fun save(calibration: Calibration) {
        context.dataStore.edit { prefs ->
            prefs[Keys.METHOD] = calibration.method.rawValue
            prefs[Keys.PIXELS_PER_METER] = calibration.pixelsPerMeter
            prefs[Keys.REFERENCE_DISTANCE_METERS] = calibration.referenceDistanceMeters
            prefs[Keys.PIXEL_DISTANCE] = calibration.pixelDistance
            calibration.vehicleReferenceName?.let { prefs[Keys.VEHICLE_REF_NAME] = it }
            calibration.calibrationImageWidth?.let { prefs[Keys.CALIBRATION_IMAGE_WIDTH] = it }
            prefs[Keys.TIMESTAMP] = calibration.timestampMillis
        }
    }

    val measurementSystem: Flow<MeasurementSystem> = context.dataStore.data.map { prefs ->
        prefs[Keys.MEASUREMENT_SYSTEM]?.let { MeasurementSystem.fromRawValue(it) }
            ?: defaultMeasurementSystem()
    }

    suspend fun saveMeasurementSystem(system: MeasurementSystem) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MEASUREMENT_SYSTEM] = system.rawValue
        }
    }

    private fun defaultMeasurementSystem(): MeasurementSystem {
        val country = java.util.Locale.getDefault().country
        return if (country in setOf("US", "LR", "MM")) MeasurementSystem.IMPERIAL
        else MeasurementSystem.METRIC
    }
}
