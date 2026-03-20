import Foundation
import SwiftData

enum SeedData {
    private static let seededKey = "isDemoDataSeeded"

    static var isSeeded: Bool {
        UserDefaults.standard.bool(forKey: seededKey)
    }

    static func clearDemoData(context: ModelContext) {
        let descriptor = FetchDescriptor<SpeedEntry>()
        if let entries = try? context.fetch(descriptor) {
            for entry in entries {
                context.delete(entry)
            }
        }
        UserDefaults.standard.set(false, forKey: seededKey)
    }

    #if DEBUG
    static let streets = [
        "Oak Street", "Maple Avenue", "Elm Drive", "Pine Road",
        "Cedar Lane", "Birch Way", "Walnut Street", "Cherry Blvd",
        "Willow Court", "Spruce Terrace",
    ]

    static func generate(count: Int = 50) -> [SpeedEntry] {
        var entries: [SpeedEntry] = []
        let calendar = Calendar.current

        for i in 0..<count {
            // Spread entries over the past 30 days
            let daysAgo = Int.random(in: 0...30)
            let hour = Int.random(in: 6...22)
            let minute = Int.random(in: 0...59)

            var components = calendar.dateComponents([.year, .month, .day], from: .now)
            components.day! -= daysAgo
            components.hour = hour
            components.minute = minute
            let timestamp = calendar.date(from: components) ?? .now

            // Realistic speed distribution: mostly 20-35, some outliers
            let baseSpeed: Double
            let roll = Double.random(in: 0...1)
            if roll < 0.15 {
                baseSpeed = Double.random(in: 15...20) // slow
            } else if roll < 0.70 {
                baseSpeed = Double.random(in: 22...30) // normal
            } else if roll < 0.90 {
                baseSpeed = Double.random(in: 30...38) // fast
            } else {
                baseSpeed = Double.random(in: 38...50) // speeder
            }

            let speedLimit = [25, 25, 25, 30, 30, 35].randomElement()!
            let vehicleType = VehicleType.allCases.randomElement()!
            let direction = TravelDirection.allCases.randomElement()!
            let street = streets[i % streets.count]

            let entry = SpeedEntry(
                speedMPH: round(baseSpeed * 10) / 10,
                speedLimit: speedLimit,
                streetName: street,
                notes: i % 7 == 0 ? "School zone" : "",
                vehicleType: vehicleType,
                direction: direction,
                calibrationMethod: .manualDistance,
                timeDeltaSeconds: Double.random(in: 0.2...1.5),
                pixelDisplacement: Double.random(in: 100...500),
                pixelsPerFoot: 30.0,
                referenceDistanceFeet: 10.0,
                latitude: 37.7749 + Double.random(in: -0.01...0.01),
                longitude: -122.4194 + Double.random(in: -0.01...0.01),
                timestamp: timestamp
            )
            entries.append(entry)
        }
        return entries
    }

    static func seedIfEmpty(context: ModelContext) {
        let descriptor = FetchDescriptor<SpeedEntry>()
        let count = (try? context.fetchCount(descriptor)) ?? 0
        if count == 0 {
            let entries = generate()
            for entry in entries {
                context.insert(entry)
            }
            UserDefaults.standard.set(true, forKey: seededKey)
        }
    }
    #endif
}
