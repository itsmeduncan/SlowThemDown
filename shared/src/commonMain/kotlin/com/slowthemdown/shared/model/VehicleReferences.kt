package com.slowthemdown.shared.model

data class VehicleReference(
    val name: String,
    val lengthFeet: Double,
    val category: VehicleCategory
)

object VehicleReferences {
    val all: List<VehicleReference> = listOf(
        // Sedans
        VehicleReference("Toyota Camry", 16.0, VehicleCategory.SEDAN),
        VehicleReference("Honda Civic", 15.4, VehicleCategory.SEDAN),
        VehicleReference("Honda Accord", 16.2, VehicleCategory.SEDAN),
        VehicleReference("Toyota Corolla", 15.3, VehicleCategory.SEDAN),
        VehicleReference("Nissan Altima", 15.9, VehicleCategory.SEDAN),
        VehicleReference("Hyundai Sonata", 15.9, VehicleCategory.SEDAN),
        // SUVs
        VehicleReference("Toyota RAV4", 15.1, VehicleCategory.SUV),
        VehicleReference("Honda CR-V", 15.1, VehicleCategory.SUV),
        VehicleReference("Ford Explorer", 16.7, VehicleCategory.SUV),
        VehicleReference("Chevy Equinox", 15.3, VehicleCategory.SUV),
        VehicleReference("Toyota Highlander", 16.2, VehicleCategory.SUV),
        VehicleReference("Jeep Grand Cherokee", 16.1, VehicleCategory.SUV),
        // Trucks
        VehicleReference("Ford F-150 (Crew Cab)", 19.3, VehicleCategory.TRUCK),
        VehicleReference("Chevy Silverado (Crew)", 19.8, VehicleCategory.TRUCK),
        VehicleReference("RAM 1500 (Crew Cab)", 19.1, VehicleCategory.TRUCK),
        VehicleReference("Toyota Tacoma (Double)", 17.8, VehicleCategory.TRUCK),
        VehicleReference("Ford Ranger (Crew)", 17.5, VehicleCategory.TRUCK),
        // Vans
        VehicleReference("Honda Odyssey", 16.9, VehicleCategory.VAN),
        VehicleReference("Toyota Sienna", 16.9, VehicleCategory.VAN),
        VehicleReference("Chrysler Pacifica", 17.0, VehicleCategory.VAN),
        // Compact
        VehicleReference("VW Golf", 14.1, VehicleCategory.COMPACT),
        VehicleReference("Mazda 3", 15.0, VehicleCategory.COMPACT),
        VehicleReference("Subaru Impreza", 14.8, VehicleCategory.COMPACT),
    )

    fun byCategory(): List<Pair<VehicleCategory, List<VehicleReference>>> =
        VehicleCategory.entries.mapNotNull { cat ->
            val vehicles = all.filter { it.category == cat }
            if (vehicles.isEmpty()) null else cat to vehicles
        }

    const val averageLength: Double = 15.5
}
