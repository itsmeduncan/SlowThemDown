import PhotosUI
import SwiftUI

struct CalibrateView: View {
    @State private var vm = CalibrationViewModel()
    @State private var showImagePicker = false
    @State private var selectedItem: PhotosPickerItem?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    statusCard
                    if vm.selectedImage != nil {
                        imageSection
                        distanceSection
                    } else {
                        pickImageSection
                    }
                }
                .padding()
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

    private var statusCard: some View {
        VStack(spacing: 8) {
            HStack {
                Image(systemName: vm.isCalibrated ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                    .foregroundStyle(vm.isCalibrated ? .green : .yellow)
                Text(vm.isCalibrated ? "Calibrated" : "Not Calibrated")
                    .font(.headline)
            }
            if vm.isCalibrated {
                Text("\(vm.calibration.pixelsPerFoot, specifier: "%.1f") px/ft")
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

            PhotosPicker(selection: $selectedItem, matching: .images) {
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
                        vm.selectedImage = await PIIBlurService.blurFaces(in: image)
                        vm.imageSize = image.size
                        vm.resetMarkers()
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
                TextField("Distance in feet", text: $vm.referenceDistanceText)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(.roundedBorder)
                Text("feet")
                    .foregroundStyle(.secondary)
            }

            // Quick-pick lane widths
            VStack(alignment: .leading, spacing: 8) {
                Text("Or use a standard lane width:")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                HStack {
                    ForEach(RoadStandards.allWidths, id: \.feet) { width in
                        Button(width.label) {
                            vm.referenceDistanceText = String(format: "%.0f", width.feet)
                        }
                        .buttonStyle(.bordered)
                        .font(.caption)
                    }
                }
            }

            Button {
                vm.saveCalibration()
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
