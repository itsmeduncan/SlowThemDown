package com.slowthemdown.android.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SpeedEntryEntity::class], version = 2, exportSchema = false)
abstract class SlowThemDownDatabase : RoomDatabase() {
    abstract fun speedEntryDao(): SpeedEntryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new metric columns
                db.execSQL("ALTER TABLE speed_entries ADD COLUMN speed_mps REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE speed_entries ADD COLUMN speed_limit_mps REAL NOT NULL DEFAULT 11.176")
                db.execSQL("ALTER TABLE speed_entries ADD COLUMN pixels_per_meter REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE speed_entries ADD COLUMN reference_distance_meters REAL NOT NULL DEFAULT 0")

                // Convert existing imperial data to metric
                db.execSQL("UPDATE speed_entries SET speed_mps = speedMPH * 0.44704")
                db.execSQL("UPDATE speed_entries SET speed_limit_mps = speedLimit * 0.44704")
                db.execSQL("UPDATE speed_entries SET pixels_per_meter = pixelsPerFoot * 3.28084")
                db.execSQL("UPDATE speed_entries SET reference_distance_meters = referenceDistanceFeet * 0.3048")
            }
        }
    }
}
