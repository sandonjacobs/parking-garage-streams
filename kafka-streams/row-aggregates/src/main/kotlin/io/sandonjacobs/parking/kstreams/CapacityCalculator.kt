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

        return garage.getParkingZonesList().find { it.getId() == zoneId }?.let { zone ->
            zone.getParkingRowsList().find { it.getId() == rowId }?.let { row ->
                RowCapacities(defaultCapacity = row.getParkingSpacesList().count { it.getType() == VehicleType.DEFAULT },
                    handicapCapacity = row.getParkingSpacesList().count { it.getType() == VehicleType.HANDICAP },
                    motorcycleCapacity = row.getParkingSpacesList().count { it.getType() == VehicleType.MOTORCYCLE })
            }
        }!!
    }
}

/**
 * Data class to hold capacity information for a row
 */
data class RowCapacities(
    val defaultCapacity: Int = 0,
    val handicapCapacity: Int = 0,
    val motorcycleCapacity: Int = 0
)

