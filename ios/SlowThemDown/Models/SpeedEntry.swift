import Foundation
import SwiftData

// MARK: - Schema Versions

enum SpeedEntrySchemaV1: VersionedSchema {
    static var versionIdentifier = Schema.Version(1, 0, 0)
    static var models: [any PersistentModel.Type] { [SpeedEntryV1.self] }

    @Model
    final class SpeedEntryV1 {
        var id: UUID
        var timestamp: Date
        var speedMPH: Double
        var speedLimit: Int
        var streetName: String
        var notes: String
        var vehicleTypeRaw: String
        var directionRaw: String
        var calibrationMethodRaw: String
        var timeDeltaSeconds: Double
        var pixelDisplacement: Double
        var pixelsPerFoot: Double
        var referenceDistanceFeet: Double
        var latitude: Double?
        var longitude: Double?

        init(
            speedMPH: Double = 0,
            speedLimit: Int = 25,
            streetName: String = "",
            notes: String = "",
            vehicleTypeRaw: String = "car",
            directionRaw: String = "left_to_right",
            calibrationMethodRaw: String = "manual_distance",
            timeDeltaSeconds: Double = 0,
            pixelDisplacement: Double = 0,
            pixelsPerFoot: Double = 0,
            referenceDistanceFeet: Double = 0,
            latitude: Double? = nil,
            longitude: Double? = nil
        ) {
            self.id = UUID()
            self.timestamp = .now
            self.speedMPH = speedMPH
            self.speedLimit = speedLimit
            self.streetName = streetName
            self.notes = notes
            self.vehicleTypeRaw = vehicleTypeRaw
            self.directionRaw = directionRaw
            self.calibrationMethodRaw = calibrationMethodRaw
            self.timeDeltaSeconds = timeDeltaSeconds
            self.pixelDisplacement = pixelDisplacement
            self.pixelsPerFoot = pixelsPerFoot
            self.referenceDistanceFeet = referenceDistanceFeet
            self.latitude = latitude
            self.longitude = longitude
        }
    }
}

enum SpeedEntrySchemaV2: VersionedSchema {
    static var versionIdentifier = Schema.Version(2, 0, 0)
    static var models: [any PersistentModel.Type] { [SpeedEntry.self] }
}

enum SpeedEntryMigrationPlan: SchemaMigrationPlan {
    static var schemas: [any VersionedSchema.Type] {
        [SpeedEntrySchemaV1.self, SpeedEntrySchemaV2.self]
    }

    static var stages: [MigrationStage] {
        [migrateV1toV2]
    }

    static let migrateV1toV2 = MigrationStage.custom(
        fromVersion: SpeedEntrySchemaV1.self,
        toVersion: SpeedEntrySchemaV2.self
    ) { context in
        // willMigrate: runs before schema change with old model
    } didMigrate: { context in
        // didMigrate: runs after schema change with new model
        // Convert imperial values to metric
        let entries = try context.fetch(FetchDescriptor<SpeedEntry>())
        for entry in entries {
            // speed field was renamed from speedMPH — value is still in MPH, convert to m/s
            entry.speed = entry.speed * 0.44704
            // speedLimit was Int in MPH, now Double in m/s
            entry.speedLimit = entry.speedLimit * 0.44704
            // pixelsPerMeter was pixelsPerFoot — convert px/ft to px/m
            entry.pixelsPerMeter = entry.pixelsPerMeter * 3.28084
            // referenceDistanceMeters was referenceDistanceFeet — convert ft to m
            entry.referenceDistanceMeters = entry.referenceDistanceMeters * 0.3048
        }
        try context.save()
    }
}

// MARK: - Current Model

@Model
final class SpeedEntry {
    var id: UUID
    var timestamp: Date
    var speed: Double
    var speedLimit: Double
    var streetName: String
    var notes: String

    // Stored as String rawValues for SwiftData compatibility
    var vehicleTypeRaw: String
    var directionRaw: String
    var calibrationMethodRaw: String

    // Measurement metadata
    var timeDeltaSeconds: Double
    var pixelDisplacement: Double
    var pixelsPerMeter: Double
    var referenceDistanceMeters: Double

    // Location
    var latitude: Double?
    var longitude: Double?

    init(
        speed: Double,
        speedLimit: Double = RoadStandards.defaultSpeedLimit,
        streetName: String = "",
        notes: String = "",
        vehicleType: VehicleType = .car,
        direction: TravelDirection = .leftToRight,
        calibrationMethod: CalibrationMethod = .manualDistance,
        timeDeltaSeconds: Double = 0,
        pixelDisplacement: Double = 0,
        pixelsPerMeter: Double = 0,
        referenceDistanceMeters: Double = 0,
        latitude: Double? = nil,
        longitude: Double? = nil,
        timestamp: Date = .now
    ) {
        self.id = UUID()
        self.timestamp = timestamp
        self.speed = speed
        self.speedLimit = speedLimit
        self.streetName = streetName
        self.notes = notes
        self.vehicleTypeRaw = vehicleType.rawValue
        self.directionRaw = direction.rawValue
        self.calibrationMethodRaw = calibrationMethod.rawValue
        self.timeDeltaSeconds = timeDeltaSeconds
        self.pixelDisplacement = pixelDisplacement
        self.pixelsPerMeter = pixelsPerMeter
        self.referenceDistanceMeters = referenceDistanceMeters
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
        speed > speedLimit
    }

    var speedCategory: SpeedCategory {
        guard speedLimit > 0 else { return .underLimit }
        let ratio = speed / speedLimit
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
