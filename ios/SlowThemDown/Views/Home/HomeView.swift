import SwiftData
import SwiftUI

struct HomeView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \SpeedEntry.timestamp, order: .reverse) private var entries: [SpeedEntry]
    @State private var captureVM = CaptureViewModel()
    @State private var logVM = LogViewModel()
    @State private var calibrationVM = CalibrationViewModel()
    @State private var locationManager = LocationManager()
    @State private var showVideoPicker = false
    @State private var showCamera = false
    @State private var showSavedConfirmation = false
    @State private var showingDemoData = SeedData.isSeeded
    @AppStorage("measurementSystem") private var measurementSystemRaw: String = MeasurementSystem.deviceDefault.rawValue

    private var measurementSystem: MeasurementSystem {
        MeasurementSystem(rawValue: measurementSystemRaw) ?? .imperial
    }

    private var captureFlowActive: Binding<Bool> {
        Binding(
            get: { captureVM.state != .selectSource },
            set: { if !$0 { captureVM.reset() } }
        )
    }

    var body: some View {
        NavigationStack {
            List {
                // Calibration warning (compact inline)
                if !calibrationVM.isCalibrated {
                    HStack(spacing: 6) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.caption)
                            .foregroundStyle(.yellow)
                        Text("Not calibrated — use vehicle reference or calibrate in Settings")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .listRowSeparator(.hidden)
                } else if calibrationVM.calibration.needsRecalibration {
                    HStack(spacing: 6) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.caption)
                            .foregroundStyle(.orange)
                        Text("Re-calibration recommended")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .listRowSeparator(.hidden)
                }

                // Capture buttons
                Section {
                    if captureVM.isLoadingVideo {
                        HStack(spacing: 12) {
                            ProgressView()
                            Text("Loading video\u{2026}")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                    } else {
                        HStack(spacing: 10) {
                            Button {
                                showCamera = true
                            } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: "video.fill")
                                    Text("Record")
                                }
                                .font(.subheadline.weight(.semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(Color.accentColor)
                                .foregroundStyle(.black)
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                            }
                            .buttonStyle(.plain)

                            Button {
                                showVideoPicker = true
                            } label: {
                                HStack(spacing: 6) {
                                    Image(systemName: "photo.on.rectangle")
                                    Text("Import")
                                }
                                .font(.subheadline.weight(.semibold))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(Color(.systemGray5))
                                .foregroundStyle(.primary)
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))

                // Demo banner
                if showingDemoData {
                    DemoBanner {
                        SeedData.clearDemoData(context: modelContext)
                        showingDemoData = false
                    }
                }

                // Log entries
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
            .searchable(text: $logVM.searchText, prompt: "Search streets or notes")
            .navigationTitle("Capture")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Toggle("Over Limit Only", isOn: $logVM.filterOverLimit)

                        Menu("Vehicle Type") {
                            Button("All") { logVM.filterVehicleType = nil }
                            ForEach(VehicleType.allCases, id: \.self) { type in
                                Button(type.label) { logVM.filterVehicleType = type }
                            }
                        }

                        Divider()

                        Button(logVM.sortNewestFirst ? "Sort: Oldest First" : "Sort: Newest First") {
                            logVM.sortNewestFirst.toggle()
                        }
                    } label: {
                        Image(systemName: "line.3.horizontal.decrease.circle")
                            .accessibilityLabel("Filter and sort options")
                    }
                }
            }
            .fullScreenCover(isPresented: captureFlowActive) {
                CaptureFlowView(
                    vm: captureVM,
                    calibrationVM: calibrationVM,
                    locationManager: locationManager,
                    showSavedConfirmation: $showSavedConfirmation
                )
            }
            .sheet(isPresented: $showVideoPicker) {
                VideoLibraryPicker(
                    onVideoSelected: { url in
                        showVideoPicker = false
                        Task { await captureVM.loadVideo(url: url) }
                    },
                    onCancel: { showVideoPicker = false }
                )
            }
            .sheet(isPresented: $showCamera) {
                VideoRecorderView(
                    onVideoSelected: { url in
                        showCamera = false
                        Task { await captureVM.loadVideo(url: url) }
                    },
                    onCancel: { showCamera = false }
                )
            }
            .overlay {
                if showSavedConfirmation {
                    VStack(spacing: 8) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.largeTitle)
                            .foregroundStyle(.green)
                        Text("Capture Logged")
                            .font(.headline)
                    }
                    .padding(24)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 16))
                    .transition(.opacity)
                }
            }
            .onAppear {
                locationManager.requestPermission()
                calibrationVM.reload()
            }
        }
    }

    // MARK: - Helpers

    private var filteredEntries: [SpeedEntry] {
        logVM.filteredEntries(entries)
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
                .accessibilityLabel("Direction: \(entry.direction.label)")
        }
    }

    private func deleteEntries(at offsets: IndexSet) {
        let toDelete = offsets.map { filteredEntries[$0] }
        for entry in toDelete {
            modelContext.delete(entry)
        }
    }
}
