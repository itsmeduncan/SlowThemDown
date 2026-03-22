import SwiftUI

struct V85CardView: View {
    let v85: Double
    let speedLimit: Double
    let system: MeasurementSystem

    private var isOverLimit: Bool {
        v85 > speedLimit
    }

    var body: some View {
        VStack(spacing: 12) {
            Text("V85 Speed")
                .font(.caption)
                .foregroundStyle(.secondary)

            Text("\(UnitConverter.displaySpeed(v85, system: system), specifier: "%.1f")")
                .font(.system(size: 56, weight: .bold, design: .rounded))
                .foregroundStyle(isOverLimit ? .red : .green)

            Text(UnitConverter.speedUnit(system))
                .font(.caption)
                .foregroundStyle(.secondary)

            Divider()

            Text("85% of vehicles travel at or below this speed. ")
                .font(.caption2)
                .foregroundStyle(.secondary)
            + Text(isOverLimit
                ? "This exceeds the \(Int(UnitConverter.displaySpeed(speedLimit, system: system))) \(UnitConverter.speedUnit(system).lowercased()) speed limit."
                : "This is within the \(Int(UnitConverter.displaySpeed(speedLimit, system: system))) \(UnitConverter.speedUnit(system).lowercased()) speed limit.")
                .font(.caption2)
                .foregroundStyle(isOverLimit ? .red : .green)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("V85 speed: \(Int(UnitConverter.displaySpeed(v85, system: system))) \(UnitConverter.speedUnit(system)), \(isOverLimit ? "exceeds" : "within") the speed limit")
    }
}
