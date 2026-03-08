package com.slowthemdown.android.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.shared.model.TrafficStats
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())

    fun generateCsvFile(entries: List<SpeedEntryEntity>): File {
        val file = File(context.cacheDir, "SlowThemDown_Report.csv")
        FileWriter(file).use { writer ->
            writer.write("Timestamp,Speed (MPH),Speed Limit,Street,Vehicle Type,Direction,Over Limit,Calibration Method,Time Delta,Notes\n")
            entries.forEach { entry ->
                val timestamp = dateFormat.format(Date(entry.timestamp))
                val street = escapeCsv(entry.streetName)
                val notes = escapeCsv(entry.notes)
                val overLimit = if (entry.isOverLimit) "Yes" else "No"
                writer.write(
                    "$timestamp,%.1f,${entry.speedLimit},$street,${entry.vehicleType.label},${entry.direction.label},$overLimit,${entry.calibrationMethod.label},%.3f,$notes\n"
                        .format(entry.speedMPH, entry.timeDeltaSeconds)
                )
            }
        }
        return file
    }

    fun generatePdfFile(entries: List<SpeedEntryEntity>, stats: TrafficStats): File {
        val file = File(context.cacheDir, "SlowThemDown_Report.pdf")
        val document = PdfDocument()

        val pageWidth = 612
        val pageHeight = 792
        val margin = 50f
        val contentWidth = pageWidth - 2 * margin

        val titlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = android.graphics.Color.WHITE
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = android.graphics.Color.WHITE
        }
        val bodyPaint = Paint().apply {
            textSize = 10f
            color = android.graphics.Color.LTGRAY
        }
        val linePaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            strokeWidth = 0.5f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        canvas.drawColor(android.graphics.Color.parseColor("#1C1B1F"))
        var y = margin

        // Title
        canvas.drawText("Slow Them Down Speed Report", margin, y + 18f, titlePaint)
        y += 30f
        val generated = displayDateFormat.format(Date())
        canvas.drawText("Generated: $generated", margin, y + 10f, bodyPaint)
        y += 30f

        // Stats summary
        canvas.drawText("Summary", margin, y + 12f, headerPaint)
        y += 20f
        val statLines = listOf(
            "Total Entries: ${stats.count}",
            "V85: %.1f MPH".format(stats.v85),
            "Mean: %.1f MPH".format(stats.mean),
            "Median: %.1f MPH".format(stats.median),
            "Min / Max: %.1f / %.1f MPH".format(stats.min, stats.max),
            "Over Limit: ${stats.overLimitCount} (%.0f%%)".format(stats.overLimitPercent),
            "Std Deviation: %.1f".format(stats.standardDeviation),
        )
        statLines.forEach { line ->
            canvas.drawText(line, margin + 10f, y + 10f, bodyPaint)
            y += 16f
        }
        y += 20f

        // Table header
        val columns = listOf("Time" to 130f, "Speed" to 60f, "Limit" to 50f, "Street" to 150f, "Vehicle" to 80f, "Over?" to 50f)

        fun drawTableHeader(c: Canvas, startY: Float): Float {
            var x = margin
            columns.forEach { (label, width) ->
                c.drawText(label, x, startY + 10f, headerPaint)
                x += width
            }
            c.drawLine(margin, startY + 14f, margin + contentWidth, startY + 14f, linePaint)
            return startY + 20f
        }

        y = drawTableHeader(canvas, y)

        // Table rows
        entries.forEach { entry ->
            if (y + 16f > pageHeight - margin) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                canvas.drawColor(android.graphics.Color.parseColor("#1C1B1F"))
                y = margin
                y = drawTableHeader(canvas, y)
            }

            var x = margin
            val time = SimpleDateFormat("M/d h:mm a", Locale.getDefault()).format(Date(entry.timestamp))
            val rowData = listOf(
                time, "%.1f".format(entry.speedMPH), "${entry.speedLimit}",
                entry.streetName.take(20), entry.vehicleType.label,
                if (entry.isOverLimit) "Yes" else "No"
            )
            rowData.forEachIndexed { i, text ->
                canvas.drawText(text, x, y + 10f, bodyPaint)
                x += columns[i].second
            }
            y += 16f
        }

        document.finishPage(page)
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
