import PhotosUI
import SwiftUI

struct CalibrateView: View {
    @State private var vm = CalibrationViewModel()
    @State private var showImagePicker = false
    @State private var selectedItem: PhotosPickerItem?
    @State private var showSavedConfirmation = false

    @AppStorage("measurementSystem") private var measurementSystemRaw: String = MeasurementSystem.deviceDefault.rawValue

    private var measurementSystem: MeasurementSystem {
        get { MeasurementSystem(rawValue: measurementSystemRaw) ?? .imperial }
        set { measurementSystemRaw = newValue.rawValue }
    }

    var body: some View {
        NavigationStack {
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(spacing: 20) {
                        unitToggle
                            .id("top")
                        statusCard
                        if showSavedConfirmation {
                            savedBanner
                        }
                        if vm.selectedImage != nil {
                            imageSection
                            distanceSection
                        } else if !showSavedConfirmation {
                            pickImageSection
                        }

                        Divider()
                            .padding(.top, 8)

                        NavigationLink {
                            LicensesView()
                        } label: {
                            HStack {
                                Image(systemName: "doc.text")
                                Text("Open Source Licenses")
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            .foregroundStyle(.primary)
                        }
                    }
                    .padding()
                }
                .scrollDismissesKeyboard(.interactively)
                .onTapGesture { UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil) }
                .onChange(of: showSavedConfirmation) { _, showing in
                    if showing {
                        withAnimation {
                            proxy.scrollTo("top", anchor: .top)
                        }
                    }
                }
            }
            .navigationTitle("Calibrate")
            .toolbar {
                if vm.isCalibrated {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Reset") { vm.clearCalibration() }
                    }
                }
            }
        }
    }

    private var savedBanner: some View {
        VStack(spacing: 8) {
            Image(systemName: "checkmark.circle.fill")
                .font(.largeTitle)
                .foregroundStyle(.green)
            Text("Calibration Saved")
                .font(.headline)
            Text("You're all set to start capturing speeds.")
                .font(.caption)
                .foregroundStyle(.secondary)
            Button("Calibrate Again") {
                showSavedConfirmation = false
            }
            .buttonStyle(.bordered)
            .padding(.top, 4)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .transition(.opacity)
    }

    private var unitToggle: some View {
        Picker("Units", selection: Binding(
            get: { measurementSystem },
            set: { measurementSystemRaw = $0.rawValue }
        )) {
            Text("Imperial").tag(MeasurementSystem.imperial)
            Text("Metric").tag(MeasurementSystem.metric)
        }
        .pickerStyle(.segmented)
    }

    private var statusCard: some View {
        VStack(spacing: 8) {
            HStack {
                if vm.calibration.needsRecalibration {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(.orange)
                    Text("Re-calibration Recommended")
                        .font(.headline)
                } else {
                    Image(systemName: vm.isCalibrated ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                        .foregroundStyle(vm.isCalibrated ? .green : .yellow)
                    Text(vm.isCalibrated ? "Calibrated" : "Not Calibrated")
                        .font(.headline)
                }
            }
            if vm.isCalibrated {
                Text("\(UnitConverter.displayPixelsPerUnit(vm.calibration.pixelsPerMeter, system: measurementSystem), specifier: "%.1f") \(UnitConverter.calibrationUnit(measurementSystem))")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text("Last calibrated: \(vm.calibration.timestamp.formatted(.dateTime.month().day().hour().minute()))")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            } else {
                Text("Take a photo of a scene with a known distance, then mark two points.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var pickImageSection: some View {
        VStack(spacing: 16) {
            Text("Step 1: Select a reference image")
                .font(.headline)

            PhotosPicker(selection: $selectedItem, matching: .all(of: [.images, .not(.livePhotos)])) {
                Label("Choose Photo", systemImage: "photo.on.rectangle")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.accentColor)
                    .foregroundStyle(.black)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .onChange(of: selectedItem) { _, newItem in
                Task {
                    if let data = try? await newItem?.loadTransferable(type: Data.self),
                       let image = UIImage(data: data) {
                        vm.selectedImage = await PIIBlurService.blurPII(in: image)
                        vm.imageSize = image.size
                        vm.resetMarkers()
                        selectedItem = nil
                    }
                }
            }
        }
    }

    private var imageSection: some View {
        VStack(spacing: 12) {
            Text("Step 2: Tap two points with a known distance apart")
                .font(.headline)
                .multilineTextAlignment(.center)

            if let image = vm.selectedImage {
                ImageMarkerOverlay(
                    image: image,
                    markers: vm.markerPoints,
                    imageSize: vm.imageSize
                ) { point, size in
                    vm.addMarker(at: point, viewSize: size)
                }
                .frame(height: 300)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            if vm.markerPoints.count == 2 {
                Text("Pixel distance: \(vm.pixelDistance, specifier: "%.1f") px")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            HStack {
                Button("Clear Markers") {
                    vm.resetMarkers()
                }
                .buttonStyle(.bordered)

                Button("Change Photo") {
                    selectedItem = nil
                    vm.selectedImage = nil
                    vm.resetMarkers()
                }
                .buttonStyle(.bordered)
            }
        }
    }

    private var distanceSection: some View {
        VStack(spacing: 16) {
            Text("Step 3: Enter the real-world distance")
                .font(.headline)

            HStack {
                TextField("Distance in \(UnitConverter.distanceUnit(measurementSystem))", text: $vm.referenceDistanceText)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(.roundedBorder)
                Text(UnitConverter.distanceUnit(measurementSystem))
                    .foregroundStyle(.secondary)
            }

            // Quick-pick lane widths
            VStack(alignment: .leading, spacing: 8) {
                Text("Or use a standard lane width:")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                HStack {
                    ForEach(RoadStandards.allWidths, id: \.meters) { width in
                        Button(RoadStandards.laneLabel(key: width.key, meters: width.meters, system: measurementSystem)) {
                            vm.referenceDistanceText = String(format: "%.1f", UnitConverter.displayDistance(width.meters, system: measurementSystem))
                        }
                        .buttonStyle(.bordered)
                        .font(.caption)
                    }
                }
            }

            Button {
                UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                vm.saveCalibration()
                withAnimation {
                    showSavedConfirmation = true
                }
            } label: {
                Label("Save Calibration", systemImage: "checkmark.circle")
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(vm.canSave ? Color.accentColor : Color.gray)
                    .foregroundStyle(.black)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .disabled(!vm.canSave)
        }
    }
}
