import Charts
import SwiftUI

struct HourlyChartView: View {
    let hourlyAverages: [HourlyAverage]
    let measurementSystem: MeasurementSystem

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Average Speed by Hour")
                .font(.headline)
            Chart(hourlyAverages) { item in
                LineMark(
                    x: .value("Hour", item.hourLabel),
                    y: .value("Avg Speed", UnitConverter.displaySpeed(item.averageSpeed, system: measurementSystem))
                )
                .foregroundStyle(Color.orange)
                .interpolationMethod(.catmullRom)

                PointMark(
                    x: .value("Hour", item.hourLabel),
                    y: .value("Avg Speed", UnitConverter.displaySpeed(item.averageSpeed, system: measurementSystem))
                )
                .foregroundStyle(Color.orange)
            }
            .frame(height: 200)
            .accessibilityLabel("Average speed by hour of day chart")
        }
        .padding()
        .background(Color(.systemGray6).opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
