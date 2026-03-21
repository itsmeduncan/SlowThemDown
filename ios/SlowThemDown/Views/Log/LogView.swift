import SwiftData
import SwiftUI

struct LogView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \SpeedEntry.timestamp, order: .reverse) private var entries: [SpeedEntry]
    @State private var vm = LogViewModel()
    @State private var showingDemoData = SeedData.isSeeded
    @AppStorage("measurementSystem") private var measurementSystemRaw: String = MeasurementSystem.deviceDefault.rawValue

    private var measurementSystem: MeasurementSystem {
        MeasurementSystem(rawValue: measurementSystemRaw) ?? .imperial
    }

    var body: some View {
        NavigationStack {
            List {
                if showingDemoData {
                    DemoBanner {
                        SeedData.clearDemoData(context: modelContext)
                        showingDemoData = false
                    }
                }

                if filteredEntries.isEmpty {
                    ContentUnavailableView(
                        "No Entries",
                        systemImage: "gauge.with.dots.needle.0percent",
                        description: Text("Capture some speed measurements to see them here.")
                    )
                } else {
                    ForEach(filteredEntries, id: \.id) { entry in
                        NavigationLink(destination: LogDetailView(entry: entry)) {
                            entryRow(entry)
                        }
                    }
                    .onDelete(perform: deleteEntries)
                }
            }
            .searchable(text: $vm.searchText, prompt: "Search streets or notes")
            .navigationTitle("Log")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Toggle("Over Limit Only", isOn: $vm.filterOverLimit)

                        Menu("Vehicle Type") {
                            Button("All") { vm.filterVehicleType = nil }
                            ForEach(VehicleType.allCases, id: \.self) { type in
                                Button(type.label) { vm.filterVehicleType = type }
                            }
                        }

                        Divider()

                        Button(vm.sortNewestFirst ? "Sort: Oldest First" : "Sort: Newest First") {
                            vm.sortNewestFirst.toggle()
                        }
                    } label: {
                        Image(systemName: "line.3.horizontal.decrease.circle")
                    }
                }
            }
        }
    }

    private var filteredEntries: [SpeedEntry] {
        vm.filteredEntries(entries)
    }

    private func entryRow(_ entry: SpeedEntry) -> some View {
        HStack {
            SpeedBadge(speed: entry.speed, speedLimit: entry.speedLimit, system: measurementSystem)

            VStack(alignment: .leading, spacing: 2) {
                if !entry.streetName.isEmpty {
                    Text(entry.streetName)
                        .font(.subheadline.bold())
                }
                HStack(spacing: 4) {
                    Image(systemName: entry.vehicleType.icon)
                        .font(.caption2)
                    Text(entry.vehicleType.label)
                        .font(.caption)
                    Text("\u{2022}")
                        .font(.caption)
                    Text("\(Int(UnitConverter.displaySpeed(entry.speedLimit, system: measurementSystem))) \(UnitConverter.speedUnit(measurementSystem).lowercased()) limit")
                        .font(.caption)
                }
                .foregroundStyle(.secondary)
                Text(entry.timestamp.formatted(.dateTime.month().day().hour().minute()))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Image(systemName: entry.direction.icon)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private func deleteEntries(at offsets: IndexSet) {
        let toDelete = offsets.map { filteredEntries[$0] }
        for entry in toDelete {
            modelContext.delete(entry)
        }
    }
}
