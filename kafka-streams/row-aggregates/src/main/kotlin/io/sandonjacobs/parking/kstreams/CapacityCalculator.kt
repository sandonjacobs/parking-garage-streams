package io.sandonjacobs.parking.kstreams

import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.ParkingSpace
import io.sandonjacobs.streaming.parking.model.VehicleType
import org.slf4j.LoggerFactory

object CapacityCalculator {

    private val logger = LoggerFactory.getLogger(CapacityCalculator::class.java)

    fun findCapacityOfRow(garage: ParkingGarage, space: ParkingSpace): RowCapacities {
        val zoneId = space.getZoneId()
        val rowId = space.getRowId()

        // Find the zone in the garage
        val zone = garage.getParkingZonesList().find { it.getId() == zoneId }
        if (zone == null) {
            logger.warn("Zone with ID {} not found in garage {}", zoneId, garage.getId())
            return RowCapacities()
        }

        // Find the row in the zone
        val row = zone.getParkingRowsList().find { it.getId() == rowId }
        if (row == null) {
            logger.warn("Row with ID {} not found in zone {}", rowId, zoneId)
            return RowCapacities()
        }

        // Count the spaces of each type in the row
        return RowCapacities(
            carCapacity = row.getParkingSpacesList().count { it.getType() == VehicleType.CAR },
            handicapCapacity = row.getParkingSpacesList().count { it.getType() == VehicleType.HANDICAP },
            motorcycleCapacity = row.getParkingSpacesList().count { it.getType() == VehicleType.MOTORCYCLE }
        )
    }
}

/**
 * Data class to hold capacity information for a row
 */
data class RowCapacities(
    val carCapacity: Int = 0,
    val handicapCapacity: Int = 0,
    val motorcycleCapacity: Int = 0
)

