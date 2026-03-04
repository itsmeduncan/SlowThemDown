import SwiftUI

struct DirectionPicker: View {
    @Binding var selection: TravelDirection

    var body: some View {
        Picker("Direction", selection: $selection) {
            ForEach(TravelDirection.allCases, id: \.self) { dir in
                Label(dir.label, systemImage: dir.icon)
                    .tag(dir)
            }
        }
    }
}
