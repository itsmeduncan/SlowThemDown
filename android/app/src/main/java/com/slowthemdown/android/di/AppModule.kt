package com.slowthemdown.android.di

import android.content.Context
import androidx.room.Room
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.slowthemdown.android.data.AgencyDirectory
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
        val builder = Room.databaseBuilder(
            context,
            SlowThemDownDatabase::class.java,
            "slowthemdown.db"
        )
            .addMigrations(SlowThemDownDatabase.MIGRATION_1_2)

        if (com.slowthemdown.android.BuildConfig.DEBUG) {
            // Pre-release only: wipe DB if migration fails on dev devices.
            // Remove before shipping v1 — production must never silently drop data.
            builder.fallbackToDestructiveMigration()
        }

        return builder.build()
    }

    @Provides
    fun provideSpeedEntryDao(db: SlowThemDownDatabase): SpeedEntryDao = db.speedEntryDao()

    @Provides
    @Singleton
    fun provideAgencyDirectory(@ApplicationContext context: Context): AgencyDirectory =
        AgencyDirectory(context)

    @Provides
    @Singleton
    fun provideFaceDetector(): FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    @Provides
    @Singleton
    fun provideTextRecognizer(): TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
}
