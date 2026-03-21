import SwiftUI

struct VehicleRefPicker: View {
    @Binding var selection: VehicleReference?

    @AppStorage("measurementSystem") private var measurementSystemRaw: String = MeasurementSystem.deviceDefault.rawValue

    private var measurementSystem: MeasurementSystem {
        MeasurementSystem(rawValue: measurementSystemRaw) ?? .imperial
    }

    var body: some View {
        List {
            ForEach(VehicleReferences.byCategory(), id: \.category) { group in
                Section(group.category.rawValue) {
                    ForEach(group.vehicles) { vehicle in
                        VehicleRefRow(
                            vehicle: vehicle,
                            isSelected: selection?.name == vehicle.name,
                            system: measurementSystem
                        ) {
                            selection = vehicle
                        }
                    }
                }
            }
        }
    }
}

private struct VehicleRefRow: View {
    let vehicle: VehicleReference
    let isSelected: Bool
    let system: MeasurementSystem
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading) {
                    Text(vehicle.name)
                        .foregroundStyle(.primary)
                    Text("\(UnitConverter.displayDistance(vehicle.lengthMeters, system: system), specifier: "%.1f") \(UnitConverter.distanceUnit(system))")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark")
                        .foregroundStyle(Color.accentColor)
                }
            }
        }
    }
}
