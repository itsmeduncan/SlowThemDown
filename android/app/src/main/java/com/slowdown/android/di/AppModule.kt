package com.slowdown.android.di

import android.content.Context
import androidx.room.Room
import com.slowdown.android.data.db.SlowDownDatabase
import com.slowdown.android.data.db.SpeedEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SlowDownDatabase {
        return Room.databaseBuilder(
            context,
            SlowDownDatabase::class.java,
            "slowdown.db"
        ).build()
    }

    @Provides
    fun provideSpeedEntryDao(db: SlowDownDatabase): SpeedEntryDao = db.speedEntryDao()
}
