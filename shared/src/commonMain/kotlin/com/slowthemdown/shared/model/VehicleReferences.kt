package com.slowthemdown.shared.model

data class VehicleReference(
    val name: String,
    val lengthMeters: Double,
    val category: VehicleCategory
)

object VehicleReferences {
    val all: List<VehicleReference> = listOf(
        // Sedans
        VehicleReference("Toyota Camry", 4.877, VehicleCategory.SEDAN),
        VehicleReference("Honda Civic", 4.694, VehicleCategory.SEDAN),
        VehicleReference("Honda Accord", 4.938, VehicleCategory.SEDAN),
        VehicleReference("Toyota Corolla", 4.663, VehicleCategory.SEDAN),
        VehicleReference("Nissan Altima", 4.846, VehicleCategory.SEDAN),
        VehicleReference("Hyundai Sonata", 4.846, VehicleCategory.SEDAN),
        // SUVs
        VehicleReference("Toyota RAV4", 4.602, VehicleCategory.SUV),
        VehicleReference("Honda CR-V", 4.602, VehicleCategory.SUV),
        VehicleReference("Ford Explorer", 5.090, VehicleCategory.SUV),
        VehicleReference("Chevy Equinox", 4.663, VehicleCategory.SUV),
        VehicleReference("Toyota Highlander", 4.938, VehicleCategory.SUV),
        VehicleReference("Jeep Grand Cherokee", 4.907, VehicleCategory.SUV),
        // Trucks
        VehicleReference("Ford F-150 (Crew Cab)", 5.883, VehicleCategory.TRUCK),
        VehicleReference("Chevy Silverado (Crew)", 6.035, VehicleCategory.TRUCK),
        VehicleReference("RAM 1500 (Crew Cab)", 5.822, VehicleCategory.TRUCK),
        VehicleReference("Toyota Tacoma (Double)", 5.425, VehicleCategory.TRUCK),
        VehicleReference("Ford Ranger (Crew)", 5.334, VehicleCategory.TRUCK),
        // Vans
        VehicleReference("Honda Odyssey", 5.151, VehicleCategory.VAN),
        VehicleReference("Toyota Sienna", 5.151, VehicleCategory.VAN),
        VehicleReference("Chrysler Pacifica", 5.182, VehicleCategory.VAN),
        // Compact
        VehicleReference("VW Golf", 4.297, VehicleCategory.COMPACT),
        VehicleReference("Mazda 3", 4.572, VehicleCategory.COMPACT),
        VehicleReference("Subaru Impreza", 4.511, VehicleCategory.COMPACT),
    )

    fun byCategory(): List<Pair<VehicleCategory, List<VehicleReference>>> =
        VehicleCategory.entries.mapNotNull { cat ->
            val vehicles = all.filter { it.category == cat }
            if (vehicles.isEmpty()) null else cat to vehicles
        }

    /** Average vehicle length in meters */
    const val averageLength: Double = 4.724
}
