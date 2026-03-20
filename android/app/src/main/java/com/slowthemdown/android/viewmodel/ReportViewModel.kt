package com.slowthemdown.android.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowthemdown.android.BuildConfig
import com.slowthemdown.android.data.db.SpeedEntryDao
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.android.debug.SeedData
import com.slowthemdown.android.service.ReportExporter
import com.slowthemdown.shared.calculator.SpeedCalculator
import com.slowthemdown.shared.model.RoadStandards
import com.slowthemdown.shared.model.TrafficStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import javax.inject.Inject

data class HistogramBucket(val label: String, val count: Int, val rangeStart: Int)

data class HourlyAverage(val hour: Int, val label: String, val averageSpeed: Double)

data class ScatterPoint(val timestampMillis: Long, val speedMPH: Double)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val dao: SpeedEntryDao,
    private val exporter: ReportExporter,
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

    val histogram: StateFlow<List<HistogramBucket>> = dao.getAllEntries()
        .map { entries ->
            if (entries.isEmpty()) return@map emptyList()
            val speeds = entries.map { it.speedMPH }
            val minBucket = ((speeds.min().toInt()) / 5) * 5
            val maxBucket = ((speeds.max().toInt()) / 5) * 5
            (minBucket..maxBucket step 5).map { start ->
                val count = speeds.count { it >= start && it < start + 5 }
                HistogramBucket("$start-${start + 5}", count, start)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hourlyAverages: StateFlow<List<HourlyAverage>> = dao.getAllEntries()
        .map { entries ->
            if (entries.isEmpty()) return@map emptyList()
            val cal = Calendar.getInstance()
            val byHour = entries.groupBy { entry ->
                cal.timeInMillis = entry.timestamp
                cal.get(Calendar.HOUR_OF_DAY)
            }
            byHour.entries
                .sortedBy { it.key }
                .map { (hour, hourEntries) ->
                    val avg = hourEntries.map { it.speedMPH }.average()
                    val label = when {
                        hour == 0 -> "12am"
                        hour < 12 -> "${hour}am"
                        hour == 12 -> "12pm"
                        else -> "${hour - 12}pm"
                    }
                    HourlyAverage(hour, label, avg)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scatterPoints: StateFlow<List<ScatterPoint>> = dao.getAllEntries()
        .map { entries ->
            entries.map { ScatterPoint(it.timestamp, it.speedMPH) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostCommonSpeedLimit: StateFlow<Int> = dao.getAllEntries()
        .map { entries ->
            if (entries.isEmpty()) RoadStandards.defaultSpeedLimit
            else entries.groupingBy { it.speedLimit }.eachCount()
                .maxByOrNull { it.value }?.key ?: RoadStandards.defaultSpeedLimit
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoadStandards.defaultSpeedLimit)

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    fun exportCsv() {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            val file = exporter.generateCsvFile(entries.value)
            _exportedFile.value = file
            _isExporting.value = false
        }
    }

    fun exportPdf() {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            val s = stats.value ?: run {
                _isExporting.value = false
                return@launch
            }
            val file = exporter.generatePdfFile(entries.value, s)
            _exportedFile.value = file
            _isExporting.value = false
        }
    }

    fun clearExportedFile() {
        _exportedFile.value = null
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = when {
            file.name.endsWith(".csv") -> "text/csv"
            file.name.endsWith(".pdf") -> "application/pdf"
            else -> "*/*"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}
