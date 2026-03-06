import SwiftUI

struct VehicleRefPicker: View {
    @Binding var selection: VehicleReference?

    var body: some View {
        List {
            ForEach(VehicleReferences.byCategory(), id: \.category) { group in
                Section(group.category.rawValue) {
                    ForEach(group.vehicles) { vehicle in
                        VehicleRefRow(
                            vehicle: vehicle,
                            isSelected: selection?.name == vehicle.name
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
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading) {
                    Text(vehicle.name)
                        .foregroundStyle(.primary)
                    Text("\(vehicle.lengthFeet, specifier: "%.1f") ft")
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
