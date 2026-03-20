import SwiftUI

struct SpeedResultView: View {
    @Bindable var vm: CaptureViewModel
    let onSave: () -> Void
    let onDiscard: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Speed readout
                VStack(spacing: 8) {
                    Text("Estimated Speed")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(vm.calculatedSpeed, specifier: "%.1f")")
                        .font(.system(size: 72, weight: .bold, design: .rounded))
                        .foregroundStyle(speedColor)
                    Text("MPH")
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
                            ForEach(RoadStandards.speedLimits, id: \.self) { limit in
                                Text("\(limit) mph").tag(limit)
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
                        Label("Save to Log", systemImage: "square.and.arrow.down")
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.accentColor)
                            .foregroundStyle(.black)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
            }
            .padding()
        }
    }

    private var speedColor: Color {
        let ratio = vm.calculatedSpeed / Double(vm.speedLimit)
        if ratio <= 1.0 { return .green }
        if ratio <= 1.2 { return .yellow }
        return .red
    }
}
