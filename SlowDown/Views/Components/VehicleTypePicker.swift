import SwiftUI

struct VehicleTypePicker: View {
    @Binding var selection: VehicleType

    var body: some View {
        Picker("Vehicle Type", selection: $selection) {
            ForEach(VehicleType.allCases, id: \.self) { type in
                Label(type.label, systemImage: type.icon)
                    .tag(type)
            }
        }
    }
}
