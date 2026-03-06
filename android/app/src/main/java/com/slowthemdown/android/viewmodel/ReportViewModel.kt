package com.slowthemdown.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowthemdown.android.data.db.SpeedEntryDao
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.shared.calculator.SpeedCalculator
import com.slowthemdown.shared.model.TrafficStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    dao: SpeedEntryDao
) : ViewModel() {

    val stats: StateFlow<TrafficStats?> = dao.getAllEntries()
        .map { entries ->
            if (entries.isEmpty()) null
            else SpeedCalculator.trafficStats(
                entries.map { it.speedMPH to it.speedLimit }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val entries: StateFlow<List<SpeedEntryEntity>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
