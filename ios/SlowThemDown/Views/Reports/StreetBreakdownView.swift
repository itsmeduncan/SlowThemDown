import SwiftUI

struct StreetBreakdownView: View {
    let streetGroups: [StreetGroup]
    let measurementSystem: MeasurementSystem
    let onSelectStreet: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("By Street")
                .font(.headline)

            ForEach(streetGroups) { group in
                Button {
                    onSelectStreet(group.name)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(group.name)
                                .font(.subheadline)
                                .fontWeight(.medium)
                            Text("\(group.count) entries \u{00B7} Avg \(String(format: "%.1f", UnitConverter.displaySpeed(group.meanSpeed, system: measurementSystem))) \(UnitConverter.speedUnit(measurementSystem))")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text(String(format: "%.0f%%", group.overLimitPercent))
                            .font(.subheadline)
                            .fontWeight(.semibold)
                            .foregroundStyle(group.overLimitPercent > 50 ? .red : .green)
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .padding()
                    .background(Color(.systemGray6).opacity(0.3))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .buttonStyle(.plain)
            }
        }
    }
}
