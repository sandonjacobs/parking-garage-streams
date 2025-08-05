package io.sandonjacobs.streaming.parking.calculator

import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.ParkingSpace
import io.sandonjacobs.streaming.parking.model.VehicleType

/**
 * Calculator for parking capacities
 */
object CapacityCalculator {

    /**
     * Calculates capacity for a zone
     */
    fun calculateZoneCapacity(garage: ParkingGarage, space: ParkingSpace): CapacityData {
        val zoneId = space.getZoneId()

        // Find the zone in the garage
        val zone = garage.getParkingZonesList().find { it.getId() == zoneId }
        if (zone == null) {
            println("Zone with ID $zoneId not found in garage ${garage.getId()}")
            return CapacityData(type = CapacityType.ZONE)
        }

        // Count the spaces of each type in all rows of the zone
        var carCapacity = 0
        var handicapCapacity = 0
        var motorcycleCapacity = 0

        // Iterate through all rows in the zone
        for (row in zone.getParkingRowsList()) {
            // Count spaces by type in this row
            carCapacity += row.getParkingSpacesList().count { it.getType() == VehicleType.CAR }
            handicapCapacity += row.getParkingSpacesList().count { it.getType() == VehicleType.HANDICAP }
            motorcycleCapacity += row.getParkingSpacesList().count { it.getType() == VehicleType.MOTORCYCLE }
        }

        return CapacityData(
            carCapacity = carCapacity,
            handicapCapacity = handicapCapacity,
            motorcycleCapacity = motorcycleCapacity,
            type = CapacityType.ZONE
        )
    }

    /**
     * Calculates capacity for a row
     */
    fun calculateRowCapacity(garage: ParkingGarage, space: ParkingSpace): CapacityData {
        val zoneId = space.getZoneId()
        val rowId = space.getRowId()

        // Find the zone in the garage
        val zone = garage.getParkingZonesList().find { it.getId() == zoneId }
        if (zone == null) {
            println("Zone with ID $zoneId not found in garage ${garage.getId()}")
            return CapacityData(type = CapacityType.ROW)
        }

        // Find the row in the zone
        val row = zone.getParkingRowsList().find { it.getId() == rowId }
        if (row == null) {
            println("Row with ID $rowId not found in zone $zoneId")
            return CapacityData(type = CapacityType.ROW)
        }

        // Count the spaces of each type in the row
        return CapacityData(
            carCapacity = row.getParkingSpacesList().count { it.getType() == VehicleType.CAR },
            handicapCapacity = row.getParkingSpacesList().count { it.getType() == VehicleType.HANDICAP },
            motorcycleCapacity = row.getParkingSpacesList().count { it.getType() == VehicleType.MOTORCYCLE },
            type = CapacityType.ROW
        )
    }
}