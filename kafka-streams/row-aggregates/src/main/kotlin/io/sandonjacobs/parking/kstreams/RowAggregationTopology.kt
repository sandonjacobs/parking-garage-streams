package io.sandonjacobs.parking.kstreams

import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.VehicleType
import io.sandonjacobs.streaming.parking.status.ParkingGarageRowStatus
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import io.sandonjacobs.streaming.parking.status.RowStatus
import io.sandonjacobs.streaming.parking.status.SpaceStatus
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

class RowAggregationTopology(private val parkingSpaceStatusSerde: Serde<ParkingSpaceStatus>,
                             private val garageSerde: Serde<ParkingGarage>,
                             private val rowAggregateSerde: Serde<ParkingGarageRowStatus>) {

    private val logger = LoggerFactory.getLogger(RowAggregationTopology::class.java)
    
    // Store the garage global table as a class member so we can access it in the aggregate function
    private lateinit var garageGlobalTable: GlobalKTable<String, ParkingGarage>

    companion object {
        const val PARKING_SPACE_STATUS_TOPIC = "parking-space-status"
        const val PARKING_GARAGE_TOPIC = "parking-garage"
        const val ROW_AGGREGATION_TOPIC = "parking-row-aggregates"
    }

    /**
     * Updates the row status based on a space status change
     * 
     * @param spaceStatus The updated space status
     * @param currentRowStatus The current row status
     * @param garage The garage containing the space
     * @return The updated row status
     */
    fun updateRowStatus(
        spaceStatus: ParkingSpaceStatus,
        currentRowStatus: ParkingGarageRowStatus,
        garage: ParkingGarage
    ): ParkingGarageRowStatus {
        val space = spaceStatus.space
        val vehicleType = space.type
        val rowCapacities = CapacityCalculator.findCapacityOfRow(garage, space)
        
        // Create a builder from the current row status
        val builder = ParkingGarageRowStatus.newBuilder(currentRowStatus)

        builder.setRowId(space.rowId)
        builder.setZoneId(space.zoneId)
        builder.setGarageId(space.garageId)
        
        // Update the capacity for each vehicle type
        val carStatusBuilder = builder.carStatusBuilder
            .setCapacity(rowCapacities.carCapacity)
            .setVehicleType(VehicleType.CAR)
        
        val handicapStatusBuilder = builder.handicapStatusBuilder
            .setCapacity(rowCapacities.handicapCapacity)
            .setVehicleType(VehicleType.HANDICAP)
        
        val motorcycleStatusBuilder = builder.motorcycleStatusBuilder
            .setCapacity(rowCapacities.motorcycleCapacity)
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
            // Create a key based on the row ID
            .selectKey { _, joined -> 
                "${joined.spaceStatus.space.garageId}-${joined.spaceStatus.space.zoneId}-${joined.spaceStatus.space.rowId}" 
            }
            // Process each record to create or update the row status
            .mapValues { _, joined ->
                val spaceStatus = joined.spaceStatus
                val garage = joined.garage
                logger.debug("joined garage id -> {}", garage.id)

                val rowId = "${spaceStatus.space.garageId}-${spaceStatus.space.zoneId}-${spaceStatus.space.rowId}"
                logger.debug("rowId = {}", rowId)

                // Create a new row status
                val carStatus = RowStatus.newBuilder()
                    .setVehicleType(VehicleType.CAR)
                    .setCapacity(0)
                    .setOccupied(0)
                    .build()
                
                val handicapStatus = RowStatus.newBuilder()
                    .setVehicleType(VehicleType.HANDICAP)
                    .setCapacity(0)
                    .setOccupied(0)
                    .build()
                
                val motorcycleStatus = RowStatus.newBuilder()
                    .setVehicleType(VehicleType.MOTORCYCLE)
                    .setCapacity(0)
                    .setOccupied(0)
                    .build()
                
                val initialRowStatus = ParkingGarageRowStatus.newBuilder()
                    .setId(rowId)
                    .setCarStatus(carStatus)
                    .setHandicapStatus(handicapStatus)
                    .setMotorcycleStatus(motorcycleStatus)
                    .build()
                
                // Update the row status with the space status
                updateRowStatus(spaceStatus, initialRowStatus, garage)
            }
            // Output to the row aggregation topic
            .to(ROW_AGGREGATION_TOPIC, Produced.with(Serdes.String(), rowAggregateSerde))
        
        return builder.build()
    }

}