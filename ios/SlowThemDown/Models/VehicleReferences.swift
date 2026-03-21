import Foundation

struct VehicleReference: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let lengthMeters: Double
    let category: VehicleCategory
}

enum VehicleCategory: String, CaseIterable {
    case sedan = "Sedan"
    case suv = "SUV"
    case truck = "Truck"
    case van = "Van"
    case compact = "Compact"
}

enum VehicleReferences {
    static let all: [VehicleReference] = [
        // Sedans
        VehicleReference(name: "Toyota Camry", lengthMeters: 4.877, category: .sedan),
        VehicleReference(name: "Honda Civic", lengthMeters: 4.694, category: .sedan),
        VehicleReference(name: "Honda Accord", lengthMeters: 4.938, category: .sedan),
        VehicleReference(name: "Toyota Corolla", lengthMeters: 4.663, category: .sedan),
        VehicleReference(name: "Nissan Altima", lengthMeters: 4.846, category: .sedan),
        VehicleReference(name: "Hyundai Sonata", lengthMeters: 4.846, category: .sedan),

        // SUVs
        VehicleReference(name: "Toyota RAV4", lengthMeters: 4.602, category: .suv),
        VehicleReference(name: "Honda CR-V", lengthMeters: 4.602, category: .suv),
        VehicleReference(name: "Ford Explorer", lengthMeters: 5.090, category: .suv),
        VehicleReference(name: "Chevy Equinox", lengthMeters: 4.663, category: .suv),
        VehicleReference(name: "Toyota Highlander", lengthMeters: 4.938, category: .suv),
        VehicleReference(name: "Jeep Grand Cherokee", lengthMeters: 4.907, category: .suv),

        // Trucks
        VehicleReference(name: "Ford F-150 (Crew Cab)", lengthMeters: 5.883, category: .truck),
        VehicleReference(name: "Chevy Silverado (Crew)", lengthMeters: 6.035, category: .truck),
        VehicleReference(name: "RAM 1500 (Crew Cab)", lengthMeters: 5.822, category: .truck),
        VehicleReference(name: "Toyota Tacoma (Double)", lengthMeters: 5.425, category: .truck),
        VehicleReference(name: "Ford Ranger (Crew)", lengthMeters: 5.334, category: .truck),

        // Vans
        VehicleReference(name: "Honda Odyssey", lengthMeters: 5.151, category: .van),
        VehicleReference(name: "Toyota Sienna", lengthMeters: 5.151, category: .van),
        VehicleReference(name: "Chrysler Pacifica", lengthMeters: 5.182, category: .van),

        // Compact
        VehicleReference(name: "VW Golf", lengthMeters: 4.297, category: .compact),
        VehicleReference(name: "Mazda 3", lengthMeters: 4.572, category: .compact),
        VehicleReference(name: "Subaru Impreza", lengthMeters: 4.511, category: .compact),
    ]

    static func byCategory() -> [(category: VehicleCategory, vehicles: [VehicleReference])] {
        VehicleCategory.allCases.compactMap { cat in
            let vehicles = all.filter { $0.category == cat }
            return vehicles.isEmpty ? nil : (cat, vehicles)
        }
    }

    /// Average vehicle length in meters
    static let averageLength: Double = 4.724
}
