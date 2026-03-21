import Foundation

enum RoadStandards {
    /// Standard residential lane widths in meters
    static let narrowLane: Double = 2.7432
    static let standardLane: Double = 3.048
    static let wideLane: Double = 3.3528
    static let arteryLane: Double = 3.6576

    /// Lane widths stored in meters; labels generated at display time
    static let allWidths: [(key: String, meters: Double)] = [
        ("narrow", narrowLane),
        ("standard", standardLane),
        ("wide", wideLane),
        ("artery", arteryLane),
    ]

    /// Returns a display label for a lane width in the user's measurement system
    static func laneLabel(key: String, meters: Double, system: MeasurementSystem) -> String {
        let displayName = key.prefix(1).uppercased() + key.dropFirst()
        switch system {
        case .imperial:
            let feet = meters * 3.28084
            return "\(displayName) (\(Int(feet)) ft)"
        case .metric:
            return "\(displayName) (\(String(format: "%.1f", meters)) m)"
        }
    }

    /// Imperial speed limits as m/s values (correspond to round MPH values)
    static let imperialSpeedLimits: [Double] = [15, 20, 25, 30, 35, 40, 45].map { $0 * 0.44704 }

    /// Metric speed limits as m/s values (correspond to round km/h values)
    static let metricSpeedLimits: [Double] = [20, 30, 40, 50, 60, 70, 80].map { $0 / 3.6 }

    /// Returns speed limit options in m/s for the user's measurement system
    static func speedLimitsForSystem(_ system: MeasurementSystem) -> [Double] {
        switch system {
        case .imperial: imperialSpeedLimits
        case .metric: metricSpeedLimits
        }
    }

    /// Default speed limit: 25 MPH = 11.176 m/s
    static let defaultSpeedLimit: Double = 11.176
}
