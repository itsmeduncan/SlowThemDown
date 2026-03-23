import SwiftData
import SwiftUI

struct CaptureFlowView: View {
    @Environment(\.modelContext) private var modelContext
    @Bindable var vm: CaptureViewModel
    var calibrationVM: CalibrationViewModel
    var locationManager: LocationManager
    @Binding var showSavedConfirmation: Bool
    @State private var showVehicleRefPicker = false

    var body: some View {
        NavigationStack {
            Group {
                switch vm.state {
                case .selectSource:
                    EmptyView() // Handled by HomeView
                case .recording:
                    EmptyView() // Handled by sheet on HomeView
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
                    SpeedResultView(vm: vm, isSaving: vm.isSaving, onSave: saveEntry, onDiscard: { vm.reset() })
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
                ToolbarItem(placement: .topBarLeading) {
                    Button("Start Over") { vm.reset() }
                }
            }
            .alert("Error", isPresented: Binding(
                get: { vm.errorMessage != nil },
                set: { if !$0 { vm.errorMessage = nil } }
            )) {
                Button("OK") { vm.errorMessage = nil }
            } message: {
                if let msg = vm.errorMessage {
                    Text(msg)
                }
            }
            .sheet(isPresented: $showVehicleRefPicker) {
                NavigationStack {
                    VehicleRefPicker(selection: $vm.selectedVehicleRef)
                        .navigationTitle("Select Vehicle")
                        .toolbar {
                            ToolbarItem(placement: .topBarTrailing) {
                                Button("Done") {
                                    showVehicleRefPicker = false
                                    vm.calculateSpeed(calibration: calibrationVM.calibration)
                                }
                                .disabled(vm.selectedVehicleRef == nil)
                            }
                        }
                }
            }
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
        vm.isSaving = true
        let entry = vm.buildEntry(calibration: calibrationVM.calibration, location: locationManager)
        modelContext.insert(entry)
        HapticManager.notification(.success)
        vm.isSaving = false
        vm.reset()
        withAnimation { showSavedConfirmation = true }
        Task {
            try? await Task.sleep(for: .seconds(1.5))
            withAnimation { showSavedConfirmation = false }
        }
    }
}
