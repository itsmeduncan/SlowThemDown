import SwiftUI

struct MetricCard: View {
    let title: String
    let value: String
    let unit: String
    var color: Color = .accentColor

    var body: some View {
        VStack(spacing: 4) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.system(.title, design: .rounded, weight: .bold))
                .foregroundStyle(color)
            Text(unit)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
