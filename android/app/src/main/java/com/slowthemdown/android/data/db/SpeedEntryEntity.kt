package com.slowthemdown.android.data.db

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
    val speedMPH: Double,
    val speedLimit: Int,
    val streetName: String = "",
    val notes: String = "",
    val vehicleTypeRaw: String = VehicleType.CAR.rawValue,
    val directionRaw: String = TravelDirection.LEFT_TO_RIGHT.rawValue,
    val calibrationMethodRaw: String = CalibrationMethod.MANUAL_DISTANCE.rawValue,
    val timeDeltaSeconds: Double = 0.0,
    val pixelDisplacement: Double = 0.0,
    val pixelsPerFoot: Double = 0.0,
    val referenceDistanceFeet: Double = 0.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val vehicleType: VehicleType get() = VehicleType.fromRawValue(vehicleTypeRaw)
    val direction: TravelDirection get() = TravelDirection.fromRawValue(directionRaw)
    val calibrationMethod: CalibrationMethod get() = CalibrationMethod.fromRawValue(calibrationMethodRaw)
    val isOverLimit: Boolean get() = speedMPH > speedLimit.toDouble()
    val speedCategory: SpeedCategory get() = SpeedCategory.fromSpeed(speedMPH, speedLimit)
}
