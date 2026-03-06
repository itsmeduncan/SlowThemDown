import UIKit

enum ReportExporter {
    static func generateCSV(entries: [SpeedEntry]) -> String {
        var csv = "Timestamp,Speed (MPH),Speed Limit,Street,Vehicle Type,Direction,Over Limit,Calibration Method,Time Delta (s),Notes\n"
        let formatter = ISO8601DateFormatter()
        for entry in entries {
            let fields = [
                formatter.string(from: entry.timestamp),
                String(format: "%.1f", entry.speedMPH),
                "\(entry.speedLimit)",
                entry.streetName.replacingOccurrences(of: ",", with: ";"),
                entry.vehicleType.label,
                entry.direction.label,
                entry.isOverLimit ? "Yes" : "No",
                entry.calibrationMethod.label,
                String(format: "%.3f", entry.timeDeltaSeconds),
                entry.notes.replacingOccurrences(of: ",", with: ";"),
            ]
            csv += fields.joined(separator: ",") + "\n"
        }
        return csv
    }

    static func generatePDF(entries: [SpeedEntry], stats: TrafficStats?) -> Data {
        let pageWidth: CGFloat = 612
        let pageHeight: CGFloat = 792
        let margin: CGFloat = 50
        let contentWidth = pageWidth - margin * 2

        let renderer = UIGraphicsPDFRenderer(
            bounds: CGRect(x: 0, y: 0, width: pageWidth, height: pageHeight)
        )

        return renderer.pdfData { context in
            context.beginPage()
            var y: CGFloat = margin

            // Title
            let titleAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 24, weight: .bold),
            ]
            let title = "Slow Them Down Speed Report"
            title.draw(at: CGPoint(x: margin, y: y), withAttributes: titleAttrs)
            y += 36

            // Date
            let dateAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 12),
                .foregroundColor: UIColor.gray,
            ]
            let dateStr = "Generated: \(Date.now.formatted(.dateTime.month().day().year().hour().minute()))"
            dateStr.draw(at: CGPoint(x: margin, y: y), withAttributes: dateAttrs)
            y += 24

            // Stats summary
            if let stats {
                let statAttrs: [NSAttributedString.Key: Any] = [
                    .font: UIFont.systemFont(ofSize: 14),
                ]
                let boldAttrs: [NSAttributedString.Key: Any] = [
                    .font: UIFont.systemFont(ofSize: 14, weight: .bold),
                ]
                let statLines = [
                    ("Total Entries:", "\(stats.count)"),
                    ("V85 Speed:", String(format: "%.1f MPH", stats.v85)),
                    ("Mean Speed:", String(format: "%.1f MPH", stats.mean)),
                    ("Median Speed:", String(format: "%.1f MPH", stats.median)),
                    ("Min / Max:", String(format: "%.1f / %.1f MPH", stats.min, stats.max)),
                    ("Over Limit:", String(format: "%d (%.0f%%)", stats.overLimitCount, stats.overLimitPercent)),
                    ("Std Deviation:", String(format: "%.1f MPH", stats.standardDeviation)),
                ]
                for (label, value) in statLines {
                    label.draw(at: CGPoint(x: margin, y: y), withAttributes: boldAttrs)
                    value.draw(at: CGPoint(x: margin + 140, y: y), withAttributes: statAttrs)
                    y += 20
                }
                y += 16
            }

            // Table header
            let headerAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 10, weight: .bold),
            ]
            let cellAttrs: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: 10),
            ]
            let columns: [(String, CGFloat)] = [
                ("Time", 0),
                ("Speed", 130),
                ("Limit", 190),
                ("Street", 240),
                ("Vehicle", 380),
                ("Over?", 450),
            ]
            for (header, x) in columns {
                header.draw(at: CGPoint(x: margin + x, y: y), withAttributes: headerAttrs)
            }
            y += 16

            // Draw line
            let linePath = UIBezierPath()
            linePath.move(to: CGPoint(x: margin, y: y))
            linePath.addLine(to: CGPoint(x: margin + contentWidth, y: y))
            UIColor.gray.setStroke()
            linePath.stroke()
            y += 4

            // Entries
            let dateFormatter = DateFormatter()
            dateFormatter.dateFormat = "M/d HH:mm"
            for entry in entries {
                if y > pageHeight - margin - 20 {
                    context.beginPage()
                    y = margin
                }
                let row: [(String, CGFloat)] = [
                    (dateFormatter.string(from: entry.timestamp), 0),
                    (String(format: "%.1f", entry.speedMPH), 130),
                    ("\(entry.speedLimit)", 190),
                    (String(entry.streetName.prefix(20)), 240),
                    (entry.vehicleType.label, 380),
                    (entry.isOverLimit ? "YES" : "", 450),
                ]
                for (text, x) in row {
                    text.draw(at: CGPoint(x: margin + x, y: y), withAttributes: cellAttrs)
                }
                y += 14
            }
        }
    }

    static func csvFileURL(entries: [SpeedEntry]) -> URL? {
        let csv = generateCSV(entries: entries)
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("SlowThemDown_Report.csv")
        do {
            try csv.write(to: url, atomically: true, encoding: .utf8)
            return url
        } catch {
            return nil
        }
    }

    static func pdfFileURL(entries: [SpeedEntry], stats: TrafficStats?) -> URL? {
        let data = generatePDF(entries: entries, stats: stats)
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("SlowThemDown_Report.pdf")
        do {
            try data.write(to: url)
            return url
        } catch {
            return nil
        }
    }
}
