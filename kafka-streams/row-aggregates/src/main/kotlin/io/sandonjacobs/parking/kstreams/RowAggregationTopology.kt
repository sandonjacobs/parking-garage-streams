package io.sandonjacobs.parking.kstreams

import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.VehicleType
import io.sandonjacobs.streaming.parking.status.ParkingGarageRowStatus
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import io.sandonjacobs.streaming.parking.status.RowStatus
import io.sandonjacobs.streaming.parking.status.SpaceStatus
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.*
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
     * Fallback method to update row status when garage data is not available
     */
    private fun updateRowStatusWithDefaults(
        spaceStatus: ParkingSpaceStatus, 
        currentRowStatus: ParkingGarageRowStatus
    ): ParkingGarageRowStatus {
        val builder = currentRowStatus.toBuilder()
        
        // Set the ID if it's not already set
        if (builder.id.isEmpty()) {
            val rowIdentifier = "${spaceStatus.space.garageId}-${spaceStatus.space.zoneId}-${spaceStatus.space.rowId}"
            builder.id = rowIdentifier
        }
        
        // Get the appropriate row status based on vehicle type
        val vehicleType = spaceStatus.space.type
        val rowStatusBuilder = when (vehicleType) {
            VehicleType.DEFAULT -> builder.defaultStatusBuilder
            VehicleType.HANDICAP -> builder.handicapStatusBuilder
            VehicleType.MOTORCYCLE -> builder.motorcycleStatusBuilder
            else -> builder.defaultStatusBuilder
        }
        
        // Only set capacity if it's not already set (capacity == 0)
        if (rowStatusBuilder.capacity == 0) {
            // Set a default capacity based on the vehicle type
            when (vehicleType) {
                VehicleType.DEFAULT -> rowStatusBuilder.capacity = 20
                VehicleType.HANDICAP -> rowStatusBuilder.capacity = 4
                VehicleType.MOTORCYCLE -> rowStatusBuilder.capacity = 1
                else -> rowStatusBuilder.capacity = 1
            }
            
            logger.debug("Setting default capacity for {} space in row {}: {}",
                vehicleType, spaceStatus.space.rowId, rowStatusBuilder.capacity)
        }
        
        // Update occupied count based on status
        when (spaceStatus.status) {
            SpaceStatus.OCCUPIED -> {
                // Increment occupied count
                rowStatusBuilder.occupied = rowStatusBuilder.occupied + 1
                logger.debug("Incrementing occupied count for {} space in row {}", 
                    vehicleType, spaceStatus.space.rowId)
            }
            SpaceStatus.VACANT -> {
                // Decrement occupied count, but don't go below 0
                if (rowStatusBuilder.occupied > 0) {
                    rowStatusBuilder.occupied = rowStatusBuilder.occupied - 1
                    logger.debug("Decrementing occupied count for {} space in row {}", 
                        vehicleType, spaceStatus.space.rowId)
                }
            }
            else -> {
                // No change for unknown status
                logger.warn("Unknown space status: {}", spaceStatus.status)
            }
        }
        
        return builder.build()
    }

    fun buildTopology(builder: StreamsBuilder): Topology {
        // Create a KStream for parking space status updates
        val spaceStatusStream = builder.stream(
            PARKING_SPACE_STATUS_TOPIC,
            Consumed.with(Serdes.String(), parkingSpaceStatusSerde)
        )
        
        // Create a GlobalKTable for parking garage data and store it as a class member
        garageGlobalTable = builder.globalTable(
            PARKING_GARAGE_TOPIC,
            Consumed.with(Serdes.String(), garageSerde)
        )

        // Join the space status stream with the garage global table
        val spaceStatusWithGarageStream = spaceStatusStream
            .leftJoin(
                garageGlobalTable,
                { _, status -> status.space.garageId }, // Key mapper to extract garage ID from space status
                { status, garage -> 
                    if (garage != null) {
                        SpaceStatusWithGarage(status, garage)
                    } else {
                        // If garage is not found, use a default garage
                        logger.warn("Garage with ID {} not found in GlobalKTable", status.space.garageId)
                        SpaceStatusWithGarage(status, ParkingGarage.getDefaultInstance())
                    }
                } // Value joiner to create a SpaceStatusWithGarage
            )
        
        // Create a custom Serde for SpaceStatusWithGarage
        val spaceStatusWithGarageSerde = Serdes.serdeFrom(
            { topic, data ->
                // Serialize SpaceStatusWithGarage to bytes
                // For simplicity, we'll just serialize the space status and ignore the garage
                // since we can look it up again from the GlobalKTable
                parkingSpaceStatusSerde.serializer().serialize(topic, data.spaceStatus)
            },
            { topic, bytes ->
                // Deserialize bytes to SpaceStatusWithGarage
                // This won't be used in our topology, but we need to provide it
                val spaceStatus = parkingSpaceStatusSerde.deserializer().deserialize(topic, bytes)
                SpaceStatusWithGarage(spaceStatus, ParkingGarage.getDefaultInstance())
            }
        )
        
        // Group the stream by row ID
        val groupedByRow = spaceStatusWithGarageStream
            .map { _, data -> 
                // Group by a composite key of garage_id-zone_id-row_id to aggregate at the row level
                val rowId = "${data.spaceStatus.space.garageId}-${data.spaceStatus.space.zoneId}-${data.spaceStatus.space.rowId}"
                logger.debug("Grouping by row ID: {}", rowId)
                KeyValue(rowId, data)
            }
            .groupByKey(Grouped.with(Serdes.String(), spaceStatusWithGarageSerde))
            
        // Aggregate the grouped stream to create row aggregates
        val rowAggregates: KTable<String, ParkingGarageRowStatus> = groupedByRow
            .aggregate(
                { createEmptyRowStatus() }, // Initializer
                { rowId, data, currentRowStatus -> 
                    logger.debug("Adding space status for row ID: {}", rowId)
                    
                    // Get the garage ID from the space status
                    val garageId = data.spaceStatus.space.garageId
                    logger.debug("Garage ID from space status: {}", garageId)
                    
                    // Use the updateRowStatus method with the garage parameter
                    val result = if (data.garage != ParkingGarage.getDefaultInstance()) {
                        updateRowStatus(data.spaceStatus, currentRowStatus, data.garage)
                    } else {
                        // Log an error but still use default values if the garage is not found
                        logger.error("Cannot process space status update for garage ID {} - garage data not found. Using default values.", 
                                    data.spaceStatus.space.garageId)
                        updateRowStatusWithDefaults(data.spaceStatus, currentRowStatus)
                    }
                    
                    logger.debug("Updated row status: DEFAULT={}/{}, HANDICAP={}/{}, MOTORCYCLE={}/{}",
                        result.defaultStatus.occupied, result.defaultStatus.capacity,
                        result.handicapStatus.occupied, result.handicapStatus.capacity,
                        result.motorcycleStatus.occupied, result.motorcycleStatus.capacity)
                    result
                },
                Materialized.with(Serdes.String(), rowAggregateSerde)
            )
            
        // Convert to stream and output to topic
        rowAggregates
            .toStream()
            .peek { rowId, rowStatus ->
                logger.debug("Row aggregate for row ID {}: DEFAULT={}/{}, HANDICAP={}/{}, MOTORCYCLE={}/{}",
                    rowId,
                    rowStatus.defaultStatus.occupied, rowStatus.defaultStatus.capacity,
                    rowStatus.handicapStatus.occupied, rowStatus.handicapStatus.capacity,
                    rowStatus.motorcycleStatus.occupied, rowStatus.motorcycleStatus.capacity)
            }
            .to(ROW_AGGREGATION_TOPIC, Produced.with(Serdes.String(), rowAggregateSerde))

        logger.info("Built RowAggregation topology")
        return builder.build()
    }
    
    /**
     * Creates an empty ParkingGarageRowStatus with zero capacity and occupied counts
     */
    private fun createEmptyRowStatus(): ParkingGarageRowStatus {
        return ParkingGarageRowStatus.newBuilder()
            .setDefaultStatus(createRowStatus(VehicleType.DEFAULT, 0, 0))
            .setHandicapStatus(createRowStatus(VehicleType.HANDICAP, 0, 0))
            .setMotorcycleStatus(createRowStatus(VehicleType.MOTORCYCLE, 0, 0))
            .build()
    }
    
    /**
     * Updates the row status based on a space status change, using capacity information from the parking garage data
     * 
     * @param spaceStatus The parking space status update
     * @param currentRowStatus The current row status
     * @param garage The parking garage data from the GlobalKTable
     * @return The updated row status
     */
    private fun updateRowStatus(
        spaceStatus: ParkingSpaceStatus, 
        currentRowStatus: ParkingGarageRowStatus,
        garage: ParkingGarage
    ): ParkingGarageRowStatus {
        val builder = currentRowStatus.toBuilder()
        
        // Set the ID if it's not already set
        if (builder.id.isEmpty()) {
            val rowIdentifier = "${spaceStatus.space.garageId}-${spaceStatus.space.zoneId}-${spaceStatus.space.rowId}"
            builder.id = rowIdentifier
        }
        
        // Get the appropriate row status based on vehicle type
        val vehicleType = spaceStatus.space.type
        val rowStatusBuilder = when (vehicleType) {
            VehicleType.DEFAULT -> builder.defaultStatusBuilder
            VehicleType.HANDICAP -> builder.handicapStatusBuilder
            VehicleType.MOTORCYCLE -> builder.motorcycleStatusBuilder
            else -> builder.defaultStatusBuilder
        }
        
//        // Only set capacities if they're not already set (capacity == 0)
//        // Capacity should never be UPDATED by the RowAgregationTopology
//        if (builder.defaultStatusBuilder.capacity == 0 ||
//            builder.handicapStatusBuilder.capacity == 0 ||
//            builder.motorcycleStatusBuilder.capacity == 0) {
//
//            // Get the garage ID from the space status
//            val garageId = spaceStatus.space.garageId
//            val zoneId = spaceStatus.space.zoneId
//            val rowId = spaceStatus.space.rowId
//
//            // Use the CapacityCalculator to calculate the row capacities from the garage data
//            val rowCapacities = CapacityCalculator.calculateRowCapacities(garage, zoneId, rowId)
//
//            // Set the capacity for all vehicle types at once
//            builder.defaultStatusBuilder.capacity = rowCapacities.defaultCapacity
//            builder.handicapStatusBuilder.capacity = rowCapacities.handicapCapacity
//            builder.motorcycleStatusBuilder.capacity = rowCapacities.motorcycleCapacity
//
//            logger.debug("Setting capacities for row {} from garage data: DEFAULT={}, HANDICAP={}, MOTORCYCLE={}",
//                rowId, rowCapacities.defaultCapacity, rowCapacities.handicapCapacity, rowCapacities.motorcycleCapacity)
//        }
        val zoneId = spaceStatus.space.zoneId
        val rowId = spaceStatus.space.rowId

        // Use the CapacityCalculator to calculate the row capacities from the garage data
//        val rowCapacities = CapacityCalculator.calculateRowCapacities(garage, zoneId, rowId)
        val rowCapacities = CapacityCalculator.findCapacityOfRow(garage, spaceStatus.space)
        builder.defaultStatusBuilder.setCapacity(rowCapacities.defaultCapacity)
        builder.handicapStatusBuilder.setCapacity(rowCapacities.handicapCapacity)
        builder.motorcycleStatusBuilder.setCapacity(rowCapacities.motorcycleCapacity)

        // Update occupied count based on status
        when (spaceStatus.status) {
            SpaceStatus.OCCUPIED -> {
                // Increment occupied count
                rowStatusBuilder.occupied = rowStatusBuilder.occupied + 1
                logger.debug("Incrementing occupied count for {} space in row {}", 
                    vehicleType, spaceStatus.space.rowId)
            }
            SpaceStatus.VACANT -> {
                // Decrement occupied count, but don't go below 0
                if (rowStatusBuilder.occupied > 0) {
                    rowStatusBuilder.occupied = rowStatusBuilder.occupied - 1
                    logger.debug("Decrementing occupied count for {} space in row {}", 
                        vehicleType, spaceStatus.space.rowId)
                }
            }
            else -> {
                // No change for unknown status
                logger.warn("Unknown space status: {}", spaceStatus.status)
            }
        }
        
        return builder.build()
    }
    
    /**
     * Creates a RowStatus object with the specified vehicle type, capacity, and occupied count
     */
    private fun createRowStatus(vehicleType: VehicleType, capacity: Int, occupied: Int = 0): RowStatus {
        return RowStatus.newBuilder()
            .setVehicleType(vehicleType)
            .setCapacity(capacity)
            .setOccupied(occupied)
            .build()
    }
}