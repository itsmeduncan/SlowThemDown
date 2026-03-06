package com.slowthemdown.android.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SpeedEntryEntity::class], version = 1, exportSchema = false)
abstract class SlowThemDownDatabase : RoomDatabase() {
    abstract fun speedEntryDao(): SpeedEntryDao
}
