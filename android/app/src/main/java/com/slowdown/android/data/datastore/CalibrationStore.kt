package com.slowdown.android.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.slowdown.shared.model.CalibrationMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Calibration(
    val method: CalibrationMethod = CalibrationMethod.MANUAL_DISTANCE,
    val pixelsPerFoot: Double = 0.0,
    val referenceDistanceFeet: Double = 0.0,
    val pixelDistance: Double = 0.0,
    val vehicleReferenceName: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    val isValid: Boolean get() = pixelsPerFoot > 0
}

private val Context.dataStore by preferencesDataStore(name = "calibration")

@Singleton
class CalibrationStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val METHOD = stringPreferencesKey("method")
        val PIXELS_PER_FOOT = doublePreferencesKey("pixels_per_foot")
        val REFERENCE_DISTANCE = doublePreferencesKey("reference_distance")
        val PIXEL_DISTANCE = doublePreferencesKey("pixel_distance")
        val VEHICLE_REF_NAME = stringPreferencesKey("vehicle_ref_name")
        val TIMESTAMP = longPreferencesKey("timestamp")
    }

    val calibration: Flow<Calibration> = context.dataStore.data.map { prefs ->
        Calibration(
            method = prefs[Keys.METHOD]?.let { CalibrationMethod.fromRawValue(it) }
                ?: CalibrationMethod.MANUAL_DISTANCE,
            pixelsPerFoot = prefs[Keys.PIXELS_PER_FOOT] ?: 0.0,
            referenceDistanceFeet = prefs[Keys.REFERENCE_DISTANCE] ?: 0.0,
            pixelDistance = prefs[Keys.PIXEL_DISTANCE] ?: 0.0,
            vehicleReferenceName = prefs[Keys.VEHICLE_REF_NAME],
            timestampMillis = prefs[Keys.TIMESTAMP] ?: System.currentTimeMillis(),
        )
    }

    suspend fun save(calibration: Calibration) {
        context.dataStore.edit { prefs ->
            prefs[Keys.METHOD] = calibration.method.rawValue
            prefs[Keys.PIXELS_PER_FOOT] = calibration.pixelsPerFoot
            prefs[Keys.REFERENCE_DISTANCE] = calibration.referenceDistanceFeet
            prefs[Keys.PIXEL_DISTANCE] = calibration.pixelDistance
            calibration.vehicleReferenceName?.let { prefs[Keys.VEHICLE_REF_NAME] = it }
            prefs[Keys.TIMESTAMP] = calibration.timestampMillis
        }
    }
}
