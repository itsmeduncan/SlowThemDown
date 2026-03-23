import SwiftUI

struct SpeedResultView: View {
    @Bindable var vm: CaptureViewModel
    var isSaving: Bool = false
    let onSave: () -> Void
    let onDiscard: () -> Void

    @AppStorage("measurementSystem") private var measurementSystemRaw: String = MeasurementSystem.deviceDefault.rawValue

    private var measurementSystem: MeasurementSystem {
        MeasurementSystem(rawValue: measurementSystemRaw) ?? .imperial
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Speed readout
                VStack(spacing: 8) {
                    Text("Estimated Speed")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(UnitConverter.displaySpeed(vm.calculatedSpeed, system: measurementSystem), specifier: "%.1f")")
                        .font(.system(size: 72, weight: .bold, design: .rounded))
                        .foregroundStyle(speedColor)
                    Text(UnitConverter.speedUnit(measurementSystem))
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }
                .padding(.top)

                // Metadata form
                VStack(spacing: 16) {
                    HStack {
                        Text("Speed Limit")
                        Spacer()
                        Picker("", selection: $vm.speedLimit) {
                            ForEach(RoadStandards.speedLimitsForSystem(measurementSystem), id: \.self) { limit in
                                Text("\(Int(UnitConverter.displaySpeed(limit, system: measurementSystem))) \(UnitConverter.speedUnit(measurementSystem))").tag(limit)
                            }
                        }
                        .pickerStyle(.menu)
                    }

                    VehicleTypePicker(selection: $vm.vehicleType)
                        .pickerStyle(.menu)

                    DirectionPicker(selection: $vm.direction)
                        .pickerStyle(.menu)

                    TextField("Nearest Intersection", text: $vm.streetName)
                        .textFieldStyle(.roundedBorder)

                    TextField("Notes (optional)", text: $vm.notes)
                        .textFieldStyle(.roundedBorder)
                }
                .padding()
                .background(Color(.systemGray6).opacity(0.3))
                .clipShape(RoundedRectangle(cornerRadius: 12))

                // Measurement details
                VStack(alignment: .leading, spacing: 4) {
                    Text("Measurement Details")
                        .font(.caption.bold())
                        .foregroundStyle(.secondary)
                    Group {
                        Text("Time delta: \(vm.timeDelta, specifier: "%.3f")s")
                        Text("Pixel displacement: \(vm.pixelDisplacement, specifier: "%.1f") px")
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // Actions
                HStack(spacing: 16) {
                    Button(role: .destructive) {
                        onDiscard()
                    } label: {
                        Label("Discard", systemImage: "trash")
                            .frame(maxWidth: .infinity)
                            .padding()
                    }
                    .buttonStyle(.bordered)

                    Button {
                        onSave()
                    } label: {
                        HStack(spacing: 8) {
                            if isSaving {
                                ProgressView()
                                    .tint(.black)
                                Text("Saving…")
                            } else {
                                Label("Save to Log", systemImage: "square.and.arrow.down")
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(isSaving ? Color.gray : Color.accentColor)
                        .foregroundStyle(.black)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                    .disabled(isSaving)
                }
            }
            .padding()
        }
    }

    private var speedColor: Color {
        guard vm.speedLimit > 0 else { return .green }
        let ratio = vm.calculatedSpeed / vm.speedLimit
        if ratio <= 1.0 { return .green }
        if ratio <= 1.2 { return .yellow }
        return .red
    }
}
