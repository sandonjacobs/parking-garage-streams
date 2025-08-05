package io.sandonjacobs.parking.kstreams

import io.sandonjacobs.streaming.parking.calculator.CapacityCalculator
import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.VehicleType
import io.sandonjacobs.streaming.parking.status.*
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.GlobalKTable
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory

/**
 * Data class to hold the joined data of a space status and its garage
 */
data class SpaceStatusWithGarage(
    val spaceStatus: ParkingSpaceStatus,
    val garage: ParkingGarage
)

class ZoneStatisticsTopology(private val parkingSpaceStatusSerde: Serde<ParkingSpaceStatus>,
                             private val garageSerde: Serde<ParkingGarage>,
                             private val zoneStatisticsSerde: Serde<ParkingGarageZoneStatus>) {

    private val logger = LoggerFactory.getLogger(ZoneStatisticsTopology::class.java)
    
    // Store the garage global table as a class member so we can access it in the aggregate function
    private lateinit var garageGlobalTable: GlobalKTable<String, ParkingGarage>

    companion object {
        const val PARKING_SPACE_STATUS_TOPIC = "parking-space-status"
        const val PARKING_GARAGE_TOPIC = "parking-garage"
        const val ZONE_STATISTICS_TOPIC = "parking-zone-aggregates"
    }

    /**
     * Updates the zone status based on a space status change
     * 
     * @param spaceStatus The updated space status
     * @param currentZoneStatus The current zone status
     * @param garage The garage containing the space
     * @return The updated zone status
     */
    fun updateZoneStatus(
        spaceStatus: ParkingSpaceStatus,
        currentZoneStatus: ParkingGarageZoneStatus,
        garage: ParkingGarage
    ): ParkingGarageZoneStatus {
        val space = spaceStatus.space
        val vehicleType = space.type
        val zoneCapacities = CapacityCalculator.calculateZoneCapacity(garage, space)
        
        // Create a builder from the current zone status
        val builder = ParkingGarageZoneStatus.newBuilder(currentZoneStatus)

        builder.setZoneId(space.zoneId)
        builder.setGarageId(space.garageId)
        
        // Update the capacity for each vehicle type
        val carStatusBuilder = builder.carStatusBuilder
            .setCapacity(zoneCapacities.carCapacity)
            .setVehicleType(VehicleType.CAR)
        
        val handicapStatusBuilder = builder.handicapStatusBuilder
            .setCapacity(zoneCapacities.handicapCapacity)
            .setVehicleType(VehicleType.HANDICAP)
        
        val motorcycleStatusBuilder = builder.motorcycleStatusBuilder
            .setCapacity(zoneCapacities.motorcycleCapacity)
            .setVehicleType(VehicleType.MOTORCYCLE)
        
        // Update the occupied count based on the space status and vehicle type
        when (vehicleType) {
            VehicleType.CAR -> {
                if (spaceStatus.status == SpaceStatus.OCCUPIED) {
                    carStatusBuilder.setOccupied(carStatusBuilder.occupied + 1)
                } else if (spaceStatus.status == SpaceStatus.VACANT && carStatusBuilder.occupied > 0) {
                    carStatusBuilder.setOccupied(carStatusBuilder.occupied - 1)
                }
            }
            VehicleType.HANDICAP -> {
                if (spaceStatus.status == SpaceStatus.OCCUPIED) {
                    handicapStatusBuilder.setOccupied(handicapStatusBuilder.occupied + 1)
                } else if (spaceStatus.status == SpaceStatus.VACANT && handicapStatusBuilder.occupied > 0) {
                    handicapStatusBuilder.setOccupied(handicapStatusBuilder.occupied - 1)
                }
            }
            VehicleType.MOTORCYCLE -> {
                if (spaceStatus.status == SpaceStatus.OCCUPIED) {
                    motorcycleStatusBuilder.setOccupied(motorcycleStatusBuilder.occupied + 1)
                } else if (spaceStatus.status == SpaceStatus.VACANT && motorcycleStatusBuilder.occupied > 0) {
                    motorcycleStatusBuilder.setOccupied(motorcycleStatusBuilder.occupied - 1)
                }
            }
            else -> {
                logger.warn("Unknown vehicle type: {}", vehicleType)
            }
        }
        
        return builder.build()
    }

    fun buildTopology(builder: StreamsBuilder): Topology {
        // Create a GlobalKTable for the garage data
        garageGlobalTable = builder.globalTable(
            PARKING_GARAGE_TOPIC,
            Consumed.with(Serdes.String(), garageSerde)
        )
        
        // Create a KStream for the space status updates
        val spaceStatusStream: KStream<String, ParkingSpaceStatus> = builder.stream(
            PARKING_SPACE_STATUS_TOPIC,
            Consumed.with(Serdes.String(), parkingSpaceStatusSerde)
        )
        
        // Join the space status stream with the garage table
        val joinedStream = spaceStatusStream
            .peek { key, value -> logger.debug("incoming space status -> {} -> {}", key, value) }
            .filter { _, spaceStatus -> spaceStatus.space != null }
            .join(
                garageGlobalTable,
                { _, spaceStatus -> spaceStatus.space.garageId }, // Key for the garage table
                { spaceStatus, garage -> SpaceStatusWithGarage(spaceStatus, garage) }
            )
        
        // Process each joined record
        joinedStream
            // Create a key based on the zone ID
            .selectKey { _, joined -> 
                "${joined.spaceStatus.space.garageId}-${joined.spaceStatus.space.zoneId}" 
            }
            // Process each record to create or update the zone status
            .mapValues { _, joined ->
                val spaceStatus = joined.spaceStatus
                val garage = joined.garage
                logger.debug("joined garage id -> {}", garage.id)

                val zoneId = "${spaceStatus.space.garageId}-${spaceStatus.space.zoneId}"
                logger.debug("zoneId = {}", zoneId)

                // Create a new zone status
                val carStatus = ZoneStatus.newBuilder()
                    .setVehicleType(VehicleType.CAR)
                    .setCapacity(0)
                    .setOccupied(0)
                    .build()
                
                val handicapStatus = ZoneStatus.newBuilder()
                    .setVehicleType(VehicleType.HANDICAP)
                    .setCapacity(0)
                    .setOccupied(0)
                    .build()

                val motorcycleStatus = ZoneStatus.newBuilder()
                    .setVehicleType(VehicleType.MOTORCYCLE)
                    .setCapacity(0)
                    .setOccupied(0)
                    .build()

                val initialZoneStatus = ParkingGarageZoneStatus.newBuilder()
                    .setId(zoneId)
                    .setCarStatus(carStatus)
                    .setHandicapStatus(handicapStatus)
                    .setMotorcycleStatus(motorcycleStatus)
                    .build()
                // Update the zone status with the space status
                updateZoneStatus(spaceStatus, initialZoneStatus, garage)
            }
            // Output to the zone statistics topic
            .to(ZONE_STATISTICS_TOPIC, Produced.with(Serdes.String(), zoneStatisticsSerde))
        
        return builder.build()
    }
}