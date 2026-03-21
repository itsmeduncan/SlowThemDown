package com.slowthemdown.android.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.slowthemdown.shared.model.CalibrationMethod
import com.slowthemdown.shared.model.SpeedCategory
import com.slowthemdown.shared.model.TravelDirection
import com.slowthemdown.shared.model.VehicleType
import java.util.UUID

@Entity(tableName = "speed_entries")
data class SpeedEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "speed_mps") val speed: Double,
    @ColumnInfo(name = "speed_limit_mps") val speedLimit: Double,
    val streetName: String = "",
    val notes: String = "",
    val vehicleTypeRaw: String = VehicleType.CAR.rawValue,
    val directionRaw: String = TravelDirection.LEFT_TO_RIGHT.rawValue,
    val calibrationMethodRaw: String = CalibrationMethod.MANUAL_DISTANCE.rawValue,
    val timeDeltaSeconds: Double = 0.0,
    val pixelDisplacement: Double = 0.0,
    @ColumnInfo(name = "pixels_per_meter") val pixelsPerMeter: Double = 0.0,
    @ColumnInfo(name = "reference_distance_meters") val referenceDistanceMeters: Double = 0.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val vehicleType: VehicleType get() = VehicleType.fromRawValue(vehicleTypeRaw)
    val direction: TravelDirection get() = TravelDirection.fromRawValue(directionRaw)
    val calibrationMethod: CalibrationMethod get() = CalibrationMethod.fromRawValue(calibrationMethodRaw)
    val isOverLimit: Boolean get() = speed > speedLimit
    val speedCategory: SpeedCategory get() = SpeedCategory.fromSpeed(speed, speedLimit)
}
