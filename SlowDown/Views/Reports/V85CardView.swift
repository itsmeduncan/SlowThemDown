import SwiftUI

struct V85CardView: View {
    let v85: Double
    let speedLimit: Int

    private var isOverLimit: Bool {
        v85 > Double(speedLimit)
    }

    var body: some View {
        VStack(spacing: 12) {
            Text("V85 Speed")
                .font(.caption)
                .foregroundStyle(.secondary)

            Text("\(v85, specifier: "%.1f")")
                .font(.system(size: 56, weight: .bold, design: .rounded))
                .foregroundStyle(isOverLimit ? .red : .green)

            Text("MPH")
                .font(.caption)
                .foregroundStyle(.secondary)

            Divider()

            Text("85% of vehicles travel at or below this speed. ")
                .font(.caption2)
                .foregroundStyle(.secondary)
            + Text(isOverLimit
                ? "This exceeds the \(speedLimit) mph speed limit."
                : "This is within the \(speedLimit) mph speed limit.")
                .font(.caption2)
                .foregroundStyle(isOverLimit ? .red : .green)
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
