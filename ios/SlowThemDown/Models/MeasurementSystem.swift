import Foundation

enum MeasurementSystem: String, CaseIterable, Codable {
    case imperial
    case metric

    static var deviceDefault: MeasurementSystem {
        let locale = Locale.current
        // US, Liberia, Myanmar use imperial
        let imperialRegions: Set<String> = ["US", "LR", "MM"]
        if let region = locale.region?.identifier, imperialRegions.contains(region) {
            return .imperial
        }
        return .metric
    }
}

enum UnitConverter {
    private static let mpsToMph = 2.23694
    private static let mpsToKmh = 3.6
    private static let metersToFeet = 3.28084

    static func displaySpeed(_ mps: Double, system: MeasurementSystem) -> Double {
        switch system {
        case .imperial: mps * mpsToMph
        case .metric: mps * mpsToKmh
        }
    }

    static func speedToMps(_ value: Double, system: MeasurementSystem) -> Double {
        switch system {
        case .imperial: value / mpsToMph
        case .metric: value / mpsToKmh
        }
    }

    static func speedUnit(_ system: MeasurementSystem) -> String {
        switch system {
        case .imperial: "MPH"
        case .metric: "km/h"
        }
    }

    static func displayDistance(_ meters: Double, system: MeasurementSystem) -> Double {
        switch system {
        case .imperial: meters * metersToFeet
        case .metric: meters
        }
    }

    static func distanceToMeters(_ value: Double, system: MeasurementSystem) -> Double {
        switch system {
        case .imperial: value / metersToFeet
        case .metric: value
        }
    }

    static func distanceUnit(_ system: MeasurementSystem) -> String {
        switch system {
        case .imperial: "ft"
        case .metric: "m"
        }
    }

    static func displayPixelsPerUnit(_ ppm: Double, system: MeasurementSystem) -> Double {
        switch system {
        case .imperial: ppm / metersToFeet
        case .metric: ppm
        }
    }

    static func calibrationUnit(_ system: MeasurementSystem) -> String {
        switch system {
        case .imperial: "px/ft"
        case .metric: "px/m"
        }
    }
}
