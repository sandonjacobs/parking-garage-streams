package io.sandonjacobs.parking.kstreams

import io.sandonjacobs.streaming.parking.calculator.CapacityCalculator
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
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.Stores
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

    /**
     * Stateful processor that maintains per-row aggregates in a state store.
     */
    private class RowAggregatorProcessor(
        private val storeName: String,
        private val updateFn: (ParkingSpaceStatus, ParkingGarageRowStatus, ParkingGarage) -> ParkingGarageRowStatus
    ) : Processor<String, SpaceStatusWithGarage, String, ParkingGarageRowStatus> {

        private lateinit var context: ProcessorContext<String, ParkingGarageRowStatus>
        private lateinit var store: KeyValueStore<String, ParkingGarageRowStatus>

        override fun init(context: ProcessorContext<String, ParkingGarageRowStatus>) {
            this.context = context
            @Suppress("UNCHECKED_CAST")
            store = context.getStateStore(storeName) as KeyValueStore<String, ParkingGarageRowStatus>
        }

        override fun process(record: Record<String, SpaceStatusWithGarage>) {
            val rowId = record.key()
            val value = record.value()
            val current = store.get(rowId) ?: ParkingGarageRowStatus.newBuilder()
                .setId(rowId)
                .setCarStatus(RowStatus.newBuilder().setVehicleType(VehicleType.CAR).setCapacity(0).setOccupied(0))
                .setHandicapStatus(RowStatus.newBuilder().setVehicleType(VehicleType.HANDICAP).setCapacity(0).setOccupied(0))
                .setMotorcycleStatus(RowStatus.newBuilder().setVehicleType(VehicleType.MOTORCYCLE).setCapacity(0).setOccupied(0))
                .build()

            val updated = updateFn(value.spaceStatus, current, value.garage)
            store.put(rowId, updated)

            context.forward(Record(rowId, updated, record.timestamp()))
        }

        override fun close() {}
    }

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
        val rowCapacities = CapacityCalculator.calculateRowCapacity(garage, space)
        
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
                    // Only increment if we haven't reached capacity
                    if (carStatusBuilder.occupied < carStatusBuilder.capacity) {
                        carStatusBuilder.setOccupied(carStatusBuilder.occupied + 1)
                    } else {
                        logger.warn("Cannot occupy car space: row {} is at capacity ({}/{})", 
                            space.rowId, carStatusBuilder.occupied, carStatusBuilder.capacity)
                    }
                } else if (spaceStatus.status == SpaceStatus.VACANT && carStatusBuilder.occupied > 0) {
                    carStatusBuilder.setOccupied(carStatusBuilder.occupied - 1)
                }
            }
            VehicleType.HANDICAP -> {
                if (spaceStatus.status == SpaceStatus.OCCUPIED) {
                    // Only increment if we haven't reached capacity
                    if (handicapStatusBuilder.occupied < handicapStatusBuilder.capacity) {
                        handicapStatusBuilder.setOccupied(handicapStatusBuilder.occupied + 1)
                    } else {
                        logger.warn("Cannot occupy handicap space: row {} is at capacity ({}/{})", 
                            space.rowId, handicapStatusBuilder.occupied, handicapStatusBuilder.capacity)
                    }
                } else if (spaceStatus.status == SpaceStatus.VACANT && handicapStatusBuilder.occupied > 0) {
                    handicapStatusBuilder.setOccupied(handicapStatusBuilder.occupied - 1)
                }
            }
            VehicleType.MOTORCYCLE -> {
                if (spaceStatus.status == SpaceStatus.OCCUPIED) {
                    // Only increment if we haven't reached capacity
                    if (motorcycleStatusBuilder.occupied < motorcycleStatusBuilder.capacity) {
                        motorcycleStatusBuilder.setOccupied(motorcycleStatusBuilder.occupied + 1)
                    } else {
                        logger.warn("Cannot occupy motorcycle space: row {} is at capacity ({}/{})", 
                            space.rowId, motorcycleStatusBuilder.occupied, motorcycleStatusBuilder.capacity)
                    }
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
        
        // Prepare state store for per-row aggregation
        val storeName = "row-aggregates-store"
        val storeBuilder = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(storeName),
            Serdes.String(),
            rowAggregateSerde
        )
        builder.addStateStore(storeBuilder)

        // Process each joined record with stateful transformer
        joinedStream
            // Create a key based on the row ID
            .selectKey { _, joined ->
                "${joined.spaceStatus.space.garageId}-${joined.spaceStatus.space.zoneId}-${joined.spaceStatus.space.rowId}"
            }
            .process(
                ProcessorSupplier {
                    RowAggregatorProcessor(storeName, this::updateRowStatus)
                },
                storeName
            )
            // Output to the row aggregation topic
            .to(ROW_AGGREGATION_TOPIC, Produced.with(Serdes.String(), rowAggregateSerde))
        
        return builder.build()
    }

}