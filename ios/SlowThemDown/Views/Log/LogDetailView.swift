import SwiftUI

struct LogDetailView: View {
    let entry: SpeedEntry

    var body: some View {
        List {
            Section("Speed") {
                row("Speed", "\(String(format: "%.1f", entry.speedMPH)) mph")
                row("Speed Limit", "\(entry.speedLimit) mph")
                row("Status", entry.speedCategory.label)
            }

            Section("Details") {
                if !entry.streetName.isEmpty {
                    row("Street", entry.streetName)
                }
                row("Vehicle", entry.vehicleType.label)
                row("Direction", entry.direction.label)
                row("Date", entry.timestamp.formatted(.dateTime.month().day().year().hour().minute()))
            }

            if !entry.notes.isEmpty {
                Section("Notes") {
                    Text(entry.notes)
                }
            }

            Section("Measurement") {
                row("Time Delta", String(format: "%.3f s", entry.timeDeltaSeconds))
                row("Pixel Displacement", String(format: "%.1f px", entry.pixelDisplacement))
                row("Pixels per Foot", String(format: "%.1f", entry.pixelsPerFoot))
                row("Reference Distance", String(format: "%.1f ft", entry.referenceDistanceFeet))
                row("Calibration", entry.calibrationMethod.label)
            }

            if let lat = entry.latitude, let lon = entry.longitude {
                Section("Location") {
                    row("Latitude", String(format: "%.6f", lat))
                    row("Longitude", String(format: "%.6f", lon))
                }
            }
        }
        .navigationTitle("Entry Details")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func row(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .foregroundStyle(.secondary)
            Spacer()
            Text(value)
        }
    }
}
