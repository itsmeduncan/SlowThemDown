import Foundation

struct VehicleReference: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let lengthFeet: Double
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
        VehicleReference(name: "Toyota Camry", lengthFeet: 16.0, category: .sedan),
        VehicleReference(name: "Honda Civic", lengthFeet: 15.4, category: .sedan),
        VehicleReference(name: "Honda Accord", lengthFeet: 16.2, category: .sedan),
        VehicleReference(name: "Toyota Corolla", lengthFeet: 15.3, category: .sedan),
        VehicleReference(name: "Nissan Altima", lengthFeet: 15.9, category: .sedan),
        VehicleReference(name: "Hyundai Sonata", lengthFeet: 15.9, category: .sedan),

        // SUVs
        VehicleReference(name: "Toyota RAV4", lengthFeet: 15.1, category: .suv),
        VehicleReference(name: "Honda CR-V", lengthFeet: 15.1, category: .suv),
        VehicleReference(name: "Ford Explorer", lengthFeet: 16.7, category: .suv),
        VehicleReference(name: "Chevy Equinox", lengthFeet: 15.3, category: .suv),
        VehicleReference(name: "Toyota Highlander", lengthFeet: 16.2, category: .suv),
        VehicleReference(name: "Jeep Grand Cherokee", lengthFeet: 16.1, category: .suv),

        // Trucks
        VehicleReference(name: "Ford F-150 (Crew Cab)", lengthFeet: 19.3, category: .truck),
        VehicleReference(name: "Chevy Silverado (Crew)", lengthFeet: 19.8, category: .truck),
        VehicleReference(name: "RAM 1500 (Crew Cab)", lengthFeet: 19.1, category: .truck),
        VehicleReference(name: "Toyota Tacoma (Double)", lengthFeet: 17.8, category: .truck),
        VehicleReference(name: "Ford Ranger (Crew)", lengthFeet: 17.5, category: .truck),

        // Vans
        VehicleReference(name: "Honda Odyssey", lengthFeet: 16.9, category: .van),
        VehicleReference(name: "Toyota Sienna", lengthFeet: 16.9, category: .van),
        VehicleReference(name: "Chrysler Pacifica", lengthFeet: 17.0, category: .van),

        // Compact
        VehicleReference(name: "VW Golf", lengthFeet: 14.1, category: .compact),
        VehicleReference(name: "Mazda 3", lengthFeet: 15.0, category: .compact),
        VehicleReference(name: "Subaru Impreza", lengthFeet: 14.8, category: .compact),
    ]

    static func byCategory() -> [(category: VehicleCategory, vehicles: [VehicleReference])] {
        VehicleCategory.allCases.compactMap { cat in
            let vehicles = all.filter { $0.category == cat }
            return vehicles.isEmpty ? nil : (cat, vehicles)
        }
    }

    /// Average vehicle length for rough estimates
    static let averageLength: Double = 15.5
}
