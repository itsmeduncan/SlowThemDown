import Foundation

enum CalibrationMethod: String, CaseIterable, Codable {
    case manualDistance = "manual_distance"
    case vehicleReference = "vehicle_reference"

    var label: String {
        switch self {
        case .manualDistance: "Manual Distance"
        case .vehicleReference: "Vehicle Reference"
        }
    }
}

enum CaptureMethod: String, CaseIterable, Codable {
    case camera = "camera"
    case library = "library"

    var label: String {
        switch self {
        case .camera: "Record Video"
        case .library: "Import from Library"
        }
    }
}

enum TravelDirection: String, CaseIterable, Codable {
    case toward = "toward"
    case away = "away"
    case leftToRight = "left_to_right"
    case rightToLeft = "right_to_left"

    var label: String {
        switch self {
        case .toward: "Toward Camera"
        case .away: "Away from Camera"
        case .leftToRight: "Left → Right"
        case .rightToLeft: "Right → Left"
        }
    }

    var icon: String {
        switch self {
        case .toward: "arrow.down"
        case .away: "arrow.up"
        case .leftToRight: "arrow.right"
        case .rightToLeft: "arrow.left"
        }
    }
}

enum VehicleType: String, CaseIterable, Codable {
    case car = "car"
    case suv = "suv"
    case truck = "truck"
    case van = "van"
    case motorcycle = "motorcycle"
    case bus = "bus"
    case other = "other"

    var label: String {
        switch self {
        case .car: "Car"
        case .suv: "SUV"
        case .truck: "Truck"
        case .van: "Van"
        case .motorcycle: "Motorcycle"
        case .bus: "Bus"
        case .other: "Other"
        }
    }

    var icon: String {
        switch self {
        case .car: "car.side"
        case .suv: "suv.side"
        case .truck: "truck.pickup.side"
        case .van: "van.fill"
        case .motorcycle: "bicycle"
        case .bus: "bus"
        case .other: "questionmark.circle"
        }
    }
}

struct Calibration: Codable, Equatable {
    var method: CalibrationMethod
    var pixelsPerFoot: Double
    var referenceDistanceFeet: Double
    var pixelDistance: Double
    var vehicleReferenceName: String?
    var timestamp: Date

    init(
        method: CalibrationMethod = .manualDistance,
        pixelsPerFoot: Double = 0,
        referenceDistanceFeet: Double = 0,
        pixelDistance: Double = 0,
        vehicleReferenceName: String? = nil,
        timestamp: Date = .now
    ) {
        self.method = method
        self.pixelsPerFoot = pixelsPerFoot
        self.referenceDistanceFeet = referenceDistanceFeet
        self.pixelDistance = pixelDistance
        self.vehicleReferenceName = vehicleReferenceName
        self.timestamp = timestamp
    }

    var isValid: Bool {
        pixelsPerFoot > 0
    }

    static let storageKey = "com.slowthemdown.calibration"

    func save() {
        if let data = try? JSONEncoder().encode(self) {
            UserDefaults.standard.set(data, forKey: Self.storageKey)
        }
    }

    static func load() -> Calibration? {
        guard let data = UserDefaults.standard.data(forKey: storageKey),
              let cal = try? JSONDecoder().decode(Calibration.self, from: data)
        else { return nil }
        return cal
    }
}
