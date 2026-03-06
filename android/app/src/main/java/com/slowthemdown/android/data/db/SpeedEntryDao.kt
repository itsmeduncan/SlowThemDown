package com.slowthemdown.android.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedEntryDao {
    @Query("SELECT * FROM speed_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<SpeedEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SpeedEntryEntity)

    @Delete
    suspend fun delete(entry: SpeedEntryEntity)

    @Query("DELETE FROM speed_entries")
    suspend fun deleteAll()

    @Query("SELECT * FROM speed_entries WHERE streetName = :street ORDER BY timestamp DESC")
    fun getEntriesByStreet(street: String): Flow<List<SpeedEntryEntity>>

    @Query("SELECT COUNT(*) FROM speed_entries")
    fun getCount(): Flow<Int>
}
