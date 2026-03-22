import Charts
import SwiftUI

struct HistogramChartView: View {
    let histogram: [SpeedBucket]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Speed Distribution")
                .font(.headline)
            Chart(histogram) { bucket in
                BarMark(
                    x: .value("Speed Range", bucket.range),
                    y: .value("Count", bucket.count)
                )
                .foregroundStyle(Color.accentColor.gradient)
            }
            .frame(height: 200)
            .accessibilityLabel("Speed distribution histogram")
        }
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
