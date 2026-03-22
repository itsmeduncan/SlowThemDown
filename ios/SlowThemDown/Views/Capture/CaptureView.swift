import SwiftData
import SwiftUI

struct CaptureView: View {
    @Environment(\.modelContext) private var modelContext
    @State private var vm = CaptureViewModel()
    @State private var calibrationVM = CalibrationViewModel()
    @State private var locationManager = LocationManager()
    @State private var showVideoPicker = false
    @State private var showCamera = false
    @State private var showVehicleRefPicker = false
    @State private var showSavedConfirmation = false

    var body: some View {
        NavigationStack {
            Group {
                switch vm.state {
                case .selectSource:
                    sourceSelectionView
                case .recording:
                    EmptyView() // Handled by sheet
                case .selectFrames:
                    FrameSelectorView(
                        frame1Time: $vm.frame1Time,
                        frame2Time: $vm.frame2Time,
                        duration: vm.videoDuration,
                        isExtracting: vm.isExtractingFrames
                    ) {
                        Task { await vm.extractFrames() }
                    }
                case .markFrame1:
                    if let image = vm.frame1Image {
                        FrameMarkerView(
                            title: "Mark Vehicle — Frame 1",
                            image: image,
                            imageSize: vm.videoSize,
                            markers: vm.frame1Markers,
                            markerColor: .blue,
                            onTap: { pt, sz in vm.addMarkerFrame1(at: pt, viewSize: sz) },
                            onConfirm: { vm.state = .markFrame2 }
                        )
                    }
                case .markFrame2:
                    if let image = vm.frame2Image {
                        VStack {
                            FrameMarkerView(
                                title: "Mark Vehicle — Frame 2",
                                image: image,
                                imageSize: vm.videoSize,
                                markers: vm.frame2Markers,
                                markerColor: .orange,
                                onTap: { pt, sz in vm.addMarkerFrame2(at: pt, viewSize: sz) },
                                onConfirm: {
                                    if vm.useVehicleReference {
                                        showVehicleRefPicker = true
                                    } else {
                                        vm.calculateSpeed(calibration: calibrationVM.calibration)
                                    }
                                }
                            )
                            vehicleRefToggle
                        }
                    }
                case .result:
                    SpeedResultView(vm: vm, onSave: saveEntry, onDiscard: { vm.reset() })
                        .task {
                            await locationManager.reverseGeocode()
                            if vm.streetName.isEmpty {
                                vm.streetName = locationManager.streetName
                            }
                        }
                }
            }
            .navigationTitle("Capture")
            .toolbar {
                if vm.state != .selectSource {
                    ToolbarItem(placement: .topBarLeading) {
                        Button("Start Over") { vm.reset() }
                    }
                }
            }
            .sheet(isPresented: $showVideoPicker) {
                VideoLibraryPicker(
                    onVideoSelected: { url in
                        showVideoPicker = false
                        Task { await vm.loadVideo(url: url) }
                    },
                    onCancel: { showVideoPicker = false }
                )
            }
            .sheet(isPresented: $showCamera) {
                VideoRecorderView(
                    onVideoSelected: { url in
                        showCamera = false
                        Task { await vm.loadVideo(url: url) }
                    },
                    onCancel: { showCamera = false }
                )
            }
            .sheet(isPresented: $showVehicleRefPicker) {
                NavigationStack {
                    VehicleRefPicker(selection: $vm.selectedVehicleRef)
                        .navigationTitle("Select Vehicle")
                        .toolbar {
                            ToolbarItem(placement: .topBarTrailing) {
                                Button("Done") {
                                    showVehicleRefPicker = false
                                    // Now need to mark vehicle reference points on frame
                                    // For simplicity, calculate with what we have
                                    vm.calculateSpeed(calibration: calibrationVM.calibration)
                                }
                                .disabled(vm.selectedVehicleRef == nil)
                            }
                        }
                }
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

    private var sourceSelectionView: some View {
        VStack(spacing: 20) {
            if !calibrationVM.isCalibrated {
                VStack(spacing: 8) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.largeTitle)
                        .foregroundStyle(.yellow)
                    Text("Not Calibrated")
                        .font(.headline)
                    Text("Go to Calibrate tab first, or use vehicle-as-reference during capture.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding()
            } else if calibrationVM.calibration.needsRecalibration {
                VStack(spacing: 8) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.largeTitle)
                        .foregroundStyle(.orange)
                    Text("Re-calibration Recommended")
                        .font(.headline)
                    Text("Your calibration was saved without image resolution data. Re-calibrate for accurate speed estimates.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding()
            }

            VStack(spacing: 16) {
                Button {
                    showCamera = true
                } label: {
                    Label("Record Video", systemImage: "video.fill")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.accentColor)
                        .foregroundStyle(.black)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }

                Button {
                    showVideoPicker = true
                } label: {
                    Label("Import from Library", systemImage: "photo.on.rectangle")
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color(.systemGray5))
                        .foregroundStyle(.primary)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding()
        }
    }

    private var vehicleRefToggle: some View {
        Toggle(isOn: $vm.useVehicleReference) {
            Label("Use vehicle as reference", systemImage: "car.side")
                .font(.caption)
        }
        .padding(.horizontal)
    }

    private func saveEntry() {
        let entry = vm.buildEntry(calibration: calibrationVM.calibration, location: locationManager)
        modelContext.insert(entry)
        HapticManager.notification(.success)
        vm.reset()
        withAnimation { showSavedConfirmation = true }
        Task {
            try? await Task.sleep(for: .seconds(1.5))
            withAnimation { showSavedConfirmation = false }
        }
    }
}
