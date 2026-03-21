package com.slowthemdown.android.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowthemdown.android.BuildConfig
import com.slowthemdown.android.data.datastore.CalibrationStore
import com.slowthemdown.android.data.db.SpeedEntryDao
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.android.debug.SeedData
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.VehicleType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder { NEWEST_FIRST, OLDEST_FIRST }

@HiltViewModel
class LogViewModel @Inject constructor(
    private val dao: SpeedEntryDao,
    private val calibrationStore: CalibrationStore,
    private val application: Application,
) : ViewModel() {

    private val _showingDemoData = MutableStateFlow(
        BuildConfig.DEBUG && SeedData.isSeeded(application)
    )
    val showingDemoData: StateFlow<Boolean> = _showingDemoData.asStateFlow()

    fun clearDemoData() {
        viewModelScope.launch {
            SeedData.clearDemoData(dao, application)
            _showingDemoData.value = false
        }
    }

    val measurementSystem: StateFlow<MeasurementSystem> = calibrationStore.measurementSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MeasurementSystem.IMPERIAL)

    private val allEntries: StateFlow<List<SpeedEntryEntity>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _overLimitOnly = MutableStateFlow(false)
    val overLimitOnly: StateFlow<Boolean> = _overLimitOnly.asStateFlow()

    private val _vehicleTypeFilter = MutableStateFlow<VehicleType?>(null)
    val vehicleTypeFilter: StateFlow<VehicleType?> = _vehicleTypeFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val entries: StateFlow<List<SpeedEntryEntity>> = combine(
        allEntries, _searchText, _overLimitOnly, _vehicleTypeFilter, _sortOrder
    ) { entries, search, overLimit, vehicleType, sort ->
        var filtered = entries

        if (search.isNotBlank()) {
            val query = search.lowercase()
            filtered = filtered.filter {
                it.streetName.lowercase().contains(query) ||
                    it.notes.lowercase().contains(query)
            }
        }

        if (overLimit) {
            filtered = filtered.filter { it.isOverLimit }
        }

        vehicleType?.let { type ->
            filtered = filtered.filter { it.vehicleType == type }
        }

        when (sort) {
            SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.timestamp }
            SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchText(text: String) { _searchText.value = text }
    fun setOverLimitOnly(enabled: Boolean) { _overLimitOnly.value = enabled }
    fun setVehicleTypeFilter(type: VehicleType?) { _vehicleTypeFilter.value = type }
    fun toggleSortOrder() {
        _sortOrder.value = when (_sortOrder.value) {
            SortOrder.NEWEST_FIRST -> SortOrder.OLDEST_FIRST
            SortOrder.OLDEST_FIRST -> SortOrder.NEWEST_FIRST
        }
    }

    fun delete(entry: SpeedEntryEntity) {
        viewModelScope.launch { dao.delete(entry) }
    }
}
