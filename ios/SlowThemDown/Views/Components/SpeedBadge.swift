import SwiftUI

struct SpeedBadge: View {
    let speed: Double
    let speedLimit: Double
    let system: MeasurementSystem

    private var category: SpeedCategory {
        guard speedLimit > 0 else { return .underLimit }
        let ratio = speed / speedLimit
        if ratio <= 1.0 { return .underLimit }
        if ratio <= 1.2 { return .marginal }
        return .overLimit
    }

    private var color: Color {
        switch category {
        case .underLimit: .green
        case .marginal: .yellow
        case .overLimit: .red
        }
    }

    var body: some View {
        Text("\(UnitConverter.displaySpeed(speed, system: system), specifier: "%.0f")")
            .font(.system(.title3, design: .rounded, weight: .bold))
            .foregroundStyle(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(color, in: RoundedRectangle(cornerRadius: 8))
            .accessibilityElement(children: .combine)
            .accessibilityLabel("\(Int(UnitConverter.displaySpeed(speed, system: system))) \(UnitConverter.speedUnit(system)), \(category.label)")
    }
}
