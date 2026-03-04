import Foundation
import SwiftData

@Model
final class SpeedEntry {
    var id: UUID
    var timestamp: Date
    var speedMPH: Double
    var speedLimit: Int
    var streetName: String
    var notes: String

    // Stored as String rawValues for SwiftData compatibility
    var vehicleTypeRaw: String
    var directionRaw: String
    var calibrationMethodRaw: String

    // Measurement metadata
    var timeDeltaSeconds: Double
    var pixelDisplacement: Double
    var pixelsPerFoot: Double
    var referenceDistanceFeet: Double

    // Location
    var latitude: Double?
    var longitude: Double?

    init(
        speedMPH: Double,
        speedLimit: Int = RoadStandards.defaultSpeedLimit,
        streetName: String = "",
        notes: String = "",
        vehicleType: VehicleType = .car,
        direction: TravelDirection = .leftToRight,
        calibrationMethod: CalibrationMethod = .manualDistance,
        timeDeltaSeconds: Double = 0,
        pixelDisplacement: Double = 0,
        pixelsPerFoot: Double = 0,
        referenceDistanceFeet: Double = 0,
        latitude: Double? = nil,
        longitude: Double? = nil,
        timestamp: Date = .now
    ) {
        self.id = UUID()
        self.timestamp = timestamp
        self.speedMPH = speedMPH
        self.speedLimit = speedLimit
        self.streetName = streetName
        self.notes = notes
        self.vehicleTypeRaw = vehicleType.rawValue
        self.directionRaw = direction.rawValue
        self.calibrationMethodRaw = calibrationMethod.rawValue
        self.timeDeltaSeconds = timeDeltaSeconds
        self.pixelDisplacement = pixelDisplacement
        self.pixelsPerFoot = pixelsPerFoot
        self.referenceDistanceFeet = referenceDistanceFeet
        self.latitude = latitude
        self.longitude = longitude
    }

    // MARK: - Computed Enum Accessors

    var vehicleType: VehicleType {
        get { VehicleType(rawValue: vehicleTypeRaw) ?? .car }
        set { vehicleTypeRaw = newValue.rawValue }
    }

    var direction: TravelDirection {
        get { TravelDirection(rawValue: directionRaw) ?? .leftToRight }
        set { directionRaw = newValue.rawValue }
    }

    var calibrationMethod: CalibrationMethod {
        get { CalibrationMethod(rawValue: calibrationMethodRaw) ?? .manualDistance }
        set { calibrationMethodRaw = newValue.rawValue }
    }

    var isOverLimit: Bool {
        speedMPH > Double(speedLimit)
    }

    var speedCategory: SpeedCategory {
        let ratio = speedMPH / Double(speedLimit)
        if ratio <= 1.0 { return .underLimit }
        if ratio <= 1.2 { return .marginal }
        return .overLimit
    }
}

enum SpeedCategory {
    case underLimit
    case marginal
    case overLimit

    var label: String {
        switch self {
        case .underLimit: "Under Limit"
        case .marginal: "Marginal"
        case .overLimit: "Over Limit"
        }
    }
}
