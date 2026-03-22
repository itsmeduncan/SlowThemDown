import Charts
import SwiftUI

struct ScatterChartView: View {
    let dailyEntries: [DailyEntry]
    let measurementSystem: MeasurementSystem

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Speeds Over Time")
                .font(.headline)
            Chart(dailyEntries) { item in
                PointMark(
                    x: .value("Date", item.date),
                    y: .value("Speed", UnitConverter.displaySpeed(item.speed, system: measurementSystem))
                )
                .foregroundStyle(Color.blue)
            }
            .frame(height: 200)
            .accessibilityLabel("Speeds over time scatter chart")
        }
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
