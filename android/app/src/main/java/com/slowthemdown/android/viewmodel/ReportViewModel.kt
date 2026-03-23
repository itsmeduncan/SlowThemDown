package com.slowthemdown.android.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slowthemdown.android.BuildConfig
import com.slowthemdown.android.R
import com.slowthemdown.android.data.Agency
import com.slowthemdown.android.data.AgencyDirectory
import com.slowthemdown.android.data.datastore.CalibrationStore
import com.slowthemdown.android.data.db.SpeedEntryDao
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.android.debug.SeedData
import com.slowthemdown.android.service.LocationService
import com.slowthemdown.android.service.ReportExporter
import com.slowthemdown.shared.calculator.SpeedCalculator
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.RoadStandards
import com.slowthemdown.shared.model.TrafficStats
import com.slowthemdown.shared.model.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class HistogramBucket(val label: String, val count: Int, val rangeStart: Double)

data class HourlyAverage(val hour: Int, val label: String, val averageSpeed: Double)

/** Speed stored in m/s */
data class ScatterPoint(val timestampMillis: Long, val speed: Double)

data class StreetGroup(val name: String, val count: Int, val meanSpeed: Double, val overLimitPercent: Double)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val dao: SpeedEntryDao,
    private val exporter: ReportExporter,
    private val agencyDirectory: AgencyDirectory,
    private val locationService: LocationService,
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

    private val _selectedStreet = MutableStateFlow<String?>(null)
    val selectedStreet: StateFlow<String?> = _selectedStreet.asStateFlow()

    fun selectStreet(street: String?) {
        _selectedStreet.value = street
    }

    val availableStreets: StateFlow<List<String>> = dao.getDistinctStreets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filteredEntries: StateFlow<List<SpeedEntryEntity>> =
        combine(dao.getAllEntries(), _selectedStreet) { entries, street ->
            if (street == null) entries
            else entries.filter { it.streetName == street }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entries: StateFlow<List<SpeedEntryEntity>> = filteredEntries

    val stats: StateFlow<TrafficStats?> = filteredEntries
        .map { entries ->
            if (entries.isEmpty()) null
            else SpeedCalculator.trafficStats(
                entries.map { it.speed to it.speedLimit }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val streetGroups: StateFlow<List<StreetGroup>> = dao.getAllEntries()
        .map { entries ->
            entries.filter { it.streetName.isNotEmpty() }
                .groupBy { it.streetName }
                .map { (street, streetEntries) ->
                    val mean = streetEntries.map { it.speed }.average()
                    val overCount = streetEntries.count { it.isOverLimit }
                    val overPercent = overCount.toDouble() / streetEntries.size * 100
                    StreetGroup(street, streetEntries.size, mean, overPercent)
                }
                .sortedByDescending { it.count }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val histogram: StateFlow<List<HistogramBucket>> = combine(filteredEntries, measurementSystem) { entries, system ->
        if (entries.isEmpty()) return@combine emptyList()
        val displaySpeeds = entries.map { UnitConverter.displaySpeed(it.speed, system) }
        val bucketSize = 5.0
        val minBucket = (displaySpeeds.min() / bucketSize).toInt() * bucketSize
        val maxBucket = (displaySpeeds.max() / bucketSize).toInt() * bucketSize
        var start = minBucket
        val buckets = mutableListOf<HistogramBucket>()
        while (start <= maxBucket) {
            val end = start + bucketSize
            val count = displaySpeeds.count { it >= start && it < end }
            buckets.add(HistogramBucket("${start.toInt()}-${end.toInt()}", count, start))
            start += bucketSize
        }
        buckets
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hourlyAverages: StateFlow<List<HourlyAverage>> = filteredEntries
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
                    val avg = hourEntries.map { it.speed }.average()
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

    val scatterPoints: StateFlow<List<ScatterPoint>> = filteredEntries
        .map { entries ->
            entries.map { ScatterPoint(it.timestamp, it.speed) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostCommonSpeedLimit: StateFlow<Double> = filteredEntries
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
            val system = measurementSystem.value
            val file = exporter.generateCsvFile(entries.value, system)
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
            val system = measurementSystem.value
            val file = exporter.generatePdfFile(entries.value, s, system)
            _exportedFile.value = file
            _isExporting.value = false
        }
    }

    fun clearExportedFile() {
        _exportedFile.value = null
    }

    // MARK: - Agency Reporting

    private val _matchedAgencies = MutableStateFlow<List<Agency>>(emptyList())
    val matchedAgencies: StateFlow<List<Agency>> = _matchedAgencies.asStateFlow()

    private val _showAgencyPicker = MutableStateFlow(false)
    val showAgencyPicker: StateFlow<Boolean> = _showAgencyPicker.asStateFlow()

    fun showAgencyPicker() {
        viewModelScope.launch {
            _isExporting.value = true
            val currentEntries = entries.value
            val located = currentEntries.filter { it.latitude != null && it.longitude != null }

            // Try to reverse geocode the most common coordinate
            val coord = mostCommonCoordinate(located)
            if (coord != null) {
                val location = android.location.Location("").apply {
                    latitude = coord.first
                    longitude = coord.second
                }
                val info = locationService.getLocationInfo(location)
                if (info != null) {
                    val agencies = agencyDirectory.matching(
                        city = info.city.ifEmpty { null },
                        county = info.county.ifEmpty { null },
                        state = info.state.ifEmpty { null },
                    )
                    if (agencies.isNotEmpty()) {
                        _matchedAgencies.value = agencies
                        _isExporting.value = false
                        _showAgencyPicker.value = true
                        return@launch
                    }
                }
            }

            // Fallback: try current location
            val current = locationService.getCurrentLocation()
            if (current != null) {
                val info = locationService.getLocationInfo(current)
                if (info != null) {
                    val agencies = agencyDirectory.matching(
                        city = info.city.ifEmpty { null },
                        county = info.county.ifEmpty { null },
                        state = info.state.ifEmpty { null },
                    )
                    if (agencies.isNotEmpty()) {
                        _matchedAgencies.value = agencies
                        _isExporting.value = false
                        _showAgencyPicker.value = true
                        return@launch
                    }
                }
            }

            // No location — show all agencies
            _matchedAgencies.value = agencyDirectory.all()
            _isExporting.value = false
            _showAgencyPicker.value = true
        }
    }

    fun dismissAgencyPicker() {
        _showAgencyPicker.value = false
    }

    fun composeAgencyEmail(context: Context, agency: Agency) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            val s = stats.value ?: run {
                _isExporting.value = false
                return@launch
            }
            val system = measurementSystem.value
            val currentEntries = entries.value
            val pdfFile = exporter.generatePdfFile(currentEntries, s, system)
            val csvFile = exporter.generateCsvFile(currentEntries, system)
            _isExporting.value = false

            val subject = agencyEmailSubject(currentEntries, s, system)
            val body = agencyEmailBody(agency, currentEntries, s, system)

            val provider = "${context.packageName}.fileprovider"
            val attachments = ArrayList<android.net.Uri>()
            attachments.add(FileProvider.getUriForFile(context, provider, pdfFile))
            attachments.add(FileProvider.getUriForFile(context, provider, csvFile))

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(agency.email))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, application.getString(R.string.common_send_report)))
        }
    }

    private fun agencyEmailSubject(
        entries: List<SpeedEntryEntity>,
        stats: TrafficStats,
        system: MeasurementSystem,
    ): String {
        val street = mostCommonStreet(entries)
        val limit = entries.groupingBy { it.speedLimit }.eachCount()
            .maxByOrNull { it.value }?.key ?: RoadStandards.defaultSpeedLimit
        val unit = UnitConverter.speedUnit(system)
        val v85Display = "%.0f".format(UnitConverter.displaySpeed(stats.v85, system))
        val limitDisplay = UnitConverter.displaySpeed(limit, system).toInt()
        return application.getString(R.string.agency_email_subject, street, v85Display, unit, limitDisplay, unit)
    }

    private fun agencyEmailBody(
        agency: Agency,
        entries: List<SpeedEntryEntity>,
        stats: TrafficStats,
        system: MeasurementSystem,
    ): String {
        val street = mostCommonStreet(entries)
        val limit = entries.groupingBy { it.speedLimit }.eachCount()
            .maxByOrNull { it.value }?.key ?: RoadStandards.defaultSpeedLimit
        val unit = UnitConverter.speedUnit(system)
        val limitDisplay = UnitConverter.displaySpeed(limit, system).toInt()
        val v85Display = "%.1f".format(UnitConverter.displaySpeed(stats.v85, system))
        val meanDisplay = "%.1f".format(UnitConverter.displaySpeed(stats.mean, system))
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val timestamps = entries.map { it.timestamp }.sorted()
        val start = if (timestamps.isNotEmpty()) dateFormat.format(Date(timestamps.first())) else "N/A"
        val end = if (timestamps.isNotEmpty()) dateFormat.format(Date(timestamps.last())) else "N/A"

        return listOf(
            application.getString(R.string.agency_email_greeting, agency.name),
            "",
            application.getString(R.string.agency_email_intro, street),
            "",
            application.getString(R.string.agency_email_observations, stats.count, start, end),
            "",
            application.getString(R.string.agency_email_v85, v85Display, unit),
            application.getString(R.string.agency_email_avg, meanDisplay, unit),
            application.getString(R.string.agency_email_limit, limitDisplay, unit),
            application.getString(R.string.agency_email_over_limit, stats.overLimitCount, "%.0f".format(stats.overLimitPercent)),
            "",
            application.getString(R.string.agency_email_attachment),
            "",
            application.getString(R.string.agency_email_attribution),
        ).joinToString("\n")
    }

    private fun mostCommonStreet(entries: List<SpeedEntryEntity>): String {
        return entries.map { it.streetName }.filter { it.isNotEmpty() }
            .groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key ?: "Unknown Street"
    }

    private fun mostCommonCoordinate(entries: List<SpeedEntryEntity>): Pair<Double, Double>? {
        val coords = entries.mapNotNull { entry ->
            val lat = entry.latitude ?: return@mapNotNull null
            val lon = entry.longitude ?: return@mapNotNull null
            "%.3f,%.3f".format(lat, lon)
        }
        val mostCommon = coords.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            ?: return null
        val parts = mostCommon.split(",")
        return parts[0].toDoubleOrNull()?.let { lat ->
            parts[1].toDoubleOrNull()?.let { lon -> lat to lon }
        }
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
        context.startActivity(Intent.createChooser(intent, application.getString(R.string.common_share_report)))
    }
}
