import SwiftUI

struct SpeedBadge: View {
    let speed: Double
    let speedLimit: Int

    private var category: SpeedCategory {
        let ratio = speed / Double(speedLimit)
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
        Text("\(speed, specifier: "%.0f")")
            .font(.system(.title3, design: .rounded, weight: .bold))
            .foregroundStyle(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(color, in: RoundedRectangle(cornerRadius: 8))
    }
}
