package com.slowdown.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowdown.android.data.db.SpeedEntryDao
import com.slowdown.android.data.db.SpeedEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val dao: SpeedEntryDao
) : ViewModel() {

    val entries: StateFlow<List<SpeedEntryEntity>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(entry: SpeedEntryEntity) {
        viewModelScope.launch { dao.delete(entry) }
    }
}
