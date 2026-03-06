package com.slowthemdown.android.di

import android.content.Context
import androidx.room.Room
import com.slowthemdown.android.data.db.SlowThemDownDatabase
import com.slowthemdown.android.data.db.SpeedEntryDao
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
    fun provideDatabase(@ApplicationContext context: Context): SlowThemDownDatabase {
        return Room.databaseBuilder(
            context,
            SlowThemDownDatabase::class.java,
            "slowthemdown.db"
        ).build()
    }

    @Provides
    fun provideSpeedEntryDao(db: SlowThemDownDatabase): SpeedEntryDao = db.speedEntryDao()
}
