package com.slowthemdown.android.ui.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

@Singleton
class OnboardingStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val completedKey = booleanPreferencesKey("onboarding_completed")

    val isCompleted: Flow<Boolean> = context.onboardingDataStore.data.map { prefs ->
        prefs[completedKey] ?: false
    }

    suspend fun markCompleted() {
        context.onboardingDataStore.edit { prefs ->
            prefs[completedKey] = true
        }
    }
}
