package com.slowthemdown.android

import android.app.Application
import com.slowthemdown.android.data.db.SpeedEntryDao
import com.slowthemdown.android.debug.SeedData
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SlowThemDownApplication : Application() {

    @Inject lateinit var speedEntryDao: SpeedEntryDao

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            CoroutineScope(Dispatchers.IO).launch {
                SeedData.seedIfEmpty(speedEntryDao)
            }
        }
    }
}
