package io.sandonjacobs.streaming.parking.utils

import io.sandonjacobs.streaming.parking.model.*

/**
 * Example usage of ParkingGarageFactory to demonstrate how to create parking garages.
 */
object ParkingGarageExample {

    /**
     * Creates a simple parking garage with basic zones.
     */
    fun createSimpleGarage(): ParkingGarage {
        val zoneA = ParkingGarageFactory.createParkingZone(
            zoneId = "zone-a",
            handicapCapacity = 4, // 4 handicap spaces
            motorcycleCapacity = 2, // 2 motorcycle spaces
            defaultCapacity = 44, // 44 default spaces
            garageId = "garage-1"
        )

        val zoneB = ParkingGarageFactory.createParkingZone(
            zoneId = "zone-b",
            handicapCapacity = 5, // 5 handicap spaces
            motorcycleCapacity = 2, // 2 motorcycle spaces
            defaultCapacity = 68, // 68 default spaces
            garageId = "garage-1"
        )

        val location = Location.newBuilder()
            .setLatitude("40.7589")
            .setLongitude("-73.9851")
            .build()

        return ParkingGarageFactory.createParkingGarage(
            id = "garage-1",
            parkingZones = arrayOf(zoneA, zoneB),
            location = location
        )
    }

    /**
     * Creates a multi-level parking garage with structured zones.
     */
    fun createMultiLevelGarage(): ParkingGarage {
        val groundFloor = ParkingGarageFactory.createParkingZoneWithRows(
            zoneId = "ground-floor",
            numRows = 5,
            handicapCapacityPerRow = 2, // 2 handicap spaces per row
            motorcycleCapacityPerRow = 1, // 1 motorcycle space per row
            defaultCapacityPerRow = 17, // 17 default spaces per row
            garageId = "multi-level-garage"
        )

        val firstFloor = ParkingGarageFactory.createParkingZoneWithRows(
            zoneId = "first-floor",
            numRows = 4,
            handicapCapacityPerRow = 2, // 2 handicap spaces per row
            motorcycleCapacityPerRow = 1, // 1 motorcycle space per row
            defaultCapacityPerRow = 22, // 22 default spaces per row
            garageId = "multi-level-garage"
        )

        val secondFloor = ParkingGarageFactory.createParkingZoneWithRows(
            zoneId = "second-floor",
            numRows = 3,
            handicapCapacityPerRow = 2, // 2 handicap spaces per row
            motorcycleCapacityPerRow = 1, // 1 motorcycle space per row
            defaultCapacityPerRow = 27, // 27 default spaces per row
            garageId = "multi-level-garage"
        )

        val location = Location.newBuilder()
            .setLatitude("40.7505")
            .setLongitude("-73.9934")
            .build()

        return ParkingGarageFactory.createParkingGarage(
            id = "multi-level-garage",
            parkingZones = arrayOf(groundFloor, firstFloor, secondFloor),
            location = location
        )
    }

    /**
     * Creates a motorcycle-friendly parking garage.
     */
    fun createMotorcycleFriendlyGarage(): ParkingGarage {
        val motorcycleZone = ParkingGarageFactory.createParkingZone(
            zoneId = "motorcycle-zone",
            handicapCapacity = 0, // No handicap spaces
            motorcycleCapacity = 80, // 80 motorcycle spaces
            defaultCapacity = 20, // 20 default spaces
            garageId = "motorcycle-garage"
        )

        val mixedZone = ParkingGarageFactory.createParkingZone(
            zoneId = "mixed-zone",
            handicapCapacity = 5, // 5 handicap spaces
            motorcycleCapacity = 15, // 15 motorcycle spaces
            defaultCapacity = 30, // 30 default spaces
            garageId = "motorcycle-garage"
        )

        val location = Location.newBuilder()
            .setLatitude("40.7614")
            .setLongitude("-73.9776")
            .build()

        return ParkingGarageFactory.createParkingGarage(
            id = "motorcycle-garage",
            parkingZones = arrayOf(motorcycleZone, mixedZone),
            location = location
        )
    }

    /**
     * Creates a handicap-accessible parking garage.
     */
    fun createHandicapAccessibleGarage(): ParkingGarage {
        val accessibleZone = ParkingGarageFactory.createParkingZone(
            zoneId = "accessible-zone",
            handicapCapacity = 20, // 20 handicap spaces
            motorcycleCapacity = 5, // 5 motorcycle spaces
            defaultCapacity = 25, // 25 default spaces
            garageId = "accessible-garage"
        )

        val standardZone = ParkingGarageFactory.createParkingZone(
            zoneId = "standard-zone",
            handicapCapacity = 8, // 8 handicap spaces
            motorcycleCapacity = 4, // 4 motorcycle spaces
            defaultCapacity = 88, // 88 default spaces
            garageId = "accessible-garage"
        )

        val location = Location.newBuilder()
            .setLatitude("40.7527")
            .setLongitude("-73.9772")
            .build()

        return ParkingGarageFactory.createParkingGarage(
            id = "accessible-garage",
            parkingZones = arrayOf(accessibleZone, standardZone),
            location = location
        )
    }

    /**
     * Prints statistics about a parking garage.
     */
    fun printGarageStatistics(garage: ParkingGarage) {
        println("=== Parking Garage: ${garage.id} ===")
        
        if (garage.hasLocation()) {
            println("Location: ${garage.location.latitude}, ${garage.location.longitude}")
        }
        
        println("Number of zones: ${garage.parkingZonesCount}")
        
        var totalSpaces = 0
        var totalHandicapSpaces = 0
        var totalMotorcycleSpaces = 0
        var totalDefaultSpaces = 0
        
        for (zoneIndex in 0 until garage.parkingZonesCount) {
            val zone = garage.getParkingZones(zoneIndex)
            println("\n--- Zone: ${zone.id} ---")
            println("Number of rows: ${zone.parkingRowsCount}")
            
            for (rowIndex in 0 until zone.parkingRowsCount) {
                val row = zone.getParkingRows(rowIndex)
                val spaces = row.parkingSpacesList
                
                val handicapSpaces = spaces.count { it.type == VehicleType.HANDICAP }
                val motorcycleSpaces = spaces.count { it.type == VehicleType.MOTORCYCLE }
                val defaultSpaces = spaces.count { it.type == VehicleType.DEFAULT }
                
                println("  Row ${row.id}: ${spaces.size} spaces " +
                        "(${handicapSpaces} handicap, ${motorcycleSpaces} motorcycle, ${defaultSpaces} default)")
                
                totalSpaces += spaces.size
                totalHandicapSpaces += handicapSpaces
                totalMotorcycleSpaces += motorcycleSpaces
                totalDefaultSpaces += defaultSpaces
            }
        }
        
        println("\n=== Total Statistics ===")
        println("Total spaces: $totalSpaces")
        println("Handicap spaces: $totalHandicapSpaces (${String.format("%.1f", totalHandicapSpaces * 100.0 / totalSpaces)}%)")
        println("Motorcycle spaces: $totalMotorcycleSpaces (${String.format("%.1f", totalMotorcycleSpaces * 100.0 / totalSpaces)}%)")
        println("Default spaces: $totalDefaultSpaces (${String.format("%.1f", totalDefaultSpaces * 100.0 / totalSpaces)}%)")
    }
} 