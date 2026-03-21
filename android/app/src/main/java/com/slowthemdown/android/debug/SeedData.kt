package com.slowthemdown.android.debug

import android.content.Context
import com.slowthemdown.android.data.db.SpeedEntryDao
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.shared.model.CalibrationMethod
import com.slowthemdown.shared.model.TravelDirection
import com.slowthemdown.shared.model.VehicleType
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlin.random.Random

object SeedData {

    private const val PREFS_NAME = "seed_data_prefs"
    private const val KEY_SEEDED = "isDemoDataSeeded"

    fun isSeeded(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SEEDED, false)

    private fun setSeeded(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SEEDED, value).apply()
    }
    private val streets = listOf(
        "Oak Street", "Maple Avenue", "Elm Drive", "Pine Road",
        "Cedar Lane", "Birch Way", "Walnut Street", "Cherry Blvd",
        "Willow Court", "Spruce Terrace",
    )

    private val vehicleTypes = VehicleType.entries.toList()
    private val directions = TravelDirection.entries.toList()
    // Speed limits in m/s (25, 25, 25, 30, 30, 35 MPH)
    private val speedLimits = listOf(11.176, 11.176, 11.176, 13.4112, 13.4112, 15.6464)

    fun generate(count: Int = 50): List<SpeedEntryEntity> {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()

        return (0 until count).map { i ->
            val daysAgo = Random.nextInt(0, 31)
            val hour = Random.nextInt(6, 23)
            val minute = Random.nextInt(0, 60)

            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            val timestamp = calendar.timeInMillis

            val roll = Random.nextDouble()
            // Base speeds in MPH, then convert to m/s
            val baseSpeedMph = when {
                roll < 0.15 -> Random.nextDouble(15.0, 20.0)
                roll < 0.70 -> Random.nextDouble(22.0, 30.0)
                roll < 0.90 -> Random.nextDouble(30.0, 38.0)
                else -> Random.nextDouble(38.0, 50.0)
            }
            val speedMps = baseSpeedMph * 0.44704

            SpeedEntryEntity(
                timestamp = timestamp,
                speed = Math.round(speedMps * 100.0) / 100.0,
                speedLimit = speedLimits.random(),
                streetName = streets[i % streets.size],
                notes = if (i % 7 == 0) "School zone" else "",
                vehicleTypeRaw = vehicleTypes.random().rawValue,
                directionRaw = directions.random().rawValue,
                calibrationMethodRaw = CalibrationMethod.MANUAL_DISTANCE.rawValue,
                timeDeltaSeconds = Random.nextDouble(0.2, 1.5),
                pixelDisplacement = Random.nextDouble(100.0, 500.0),
                pixelsPerMeter = 98.425, // ~30 px/ft * 3.28084
                referenceDistanceMeters = 3.048, // 10 ft
                latitude = 37.7749 + Random.nextDouble(-0.01, 0.01),
                longitude = -122.4194 + Random.nextDouble(-0.01, 0.01),
            )
        }
    }

    suspend fun seedIfEmpty(dao: SpeedEntryDao, context: Context) {
        val count = dao.getCount().first()
        if (count == 0) {
            generate().forEach { dao.insert(it) }
            setSeeded(context, true)
        }
    }

    suspend fun clearDemoData(dao: SpeedEntryDao, context: Context) {
        dao.deleteAll()
        setSeeded(context, false)
    }
}
