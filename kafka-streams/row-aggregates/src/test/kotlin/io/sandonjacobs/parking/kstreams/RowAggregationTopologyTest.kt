package io.sandonjacobs.parking.kstreams

import com.google.protobuf.Timestamp
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.sandonjacobs.parking.kstreams.faker.GarageFaker
import io.sandonjacobs.parking.kstreams.serde.SerdeProvider
import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.VehicleType
import io.sandonjacobs.streaming.parking.status.ParkingGarageRowStatus
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import io.sandonjacobs.streaming.parking.status.SpaceStatus
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals

class RowAggregationTopologyTest {

    private lateinit var topology: RowAggregationTopology
    private lateinit var garageFaker: GarageFaker

    private lateinit var testDriver: TopologyTestDriver
    private lateinit var spaceStatusTopic: TestInputTopic<String, ParkingSpaceStatus>
    private lateinit var garageTopic: TestInputTopic<String, ParkingGarage>

    private lateinit var outputTopic: TestOutputTopic<String, ParkingGarageRowStatus>

    private lateinit var parkingSpaceStatusSerde: Serde<ParkingSpaceStatus>
    private lateinit var garageSerde: Serde<ParkingGarage>
    private lateinit var rowAggregateSerde: Serde<ParkingGarageRowStatus>

    private lateinit var testGarage: ParkingGarage

    // These values match the hardcoded values in RowAggregationTopology.updateRowStatusWithDefaults
    private val defaultRowCapacity = 20
    private val handicapRowCapacity = 4
    private val motorcycleRowCapacity = 1

    @BeforeEach
    fun setup() {
        val serdeProps = mapOf(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://")

        parkingSpaceStatusSerde = SerdeProvider.createProtobufSerde(ParkingSpaceStatus::class, serdeProps)
        garageSerde = SerdeProvider.createProtobufSerde(ParkingGarage::class, serdeProps)
        rowAggregateSerde = SerdeProvider.createProtobufSerde(ParkingGarageRowStatus::class, serdeProps)

        // Initialize the faker
        garageFaker = GarageFaker()
        testGarage = garageFaker.createMockGarage("g001", 2, 1,
            defaultRowCapacity, handicapRowCapacity, motorcycleRowCapacity)

        topology = RowAggregationTopology(parkingSpaceStatusSerde, garageSerde, rowAggregateSerde)
        val builder = StreamsBuilder()

        testDriver = TopologyTestDriver(topology.buildTopology(builder))

        spaceStatusTopic = testDriver.createInputTopic(RowAggregationTopology.PARKING_SPACE_STATUS_TOPIC,
            Serdes.String().serializer(), parkingSpaceStatusSerde.serializer())

        // Add a topic for the parking garage data
        garageTopic = testDriver.createInputTopic(RowAggregationTopology.PARKING_GARAGE_TOPIC,
            Serdes.String().serializer(), garageSerde.serializer())

        seedEmptyGarage(testGarage)

        outputTopic = testDriver.createOutputTopic(RowAggregationTopology.ROW_AGGREGATION_TOPIC,
            Serdes.String().deserializer(), rowAggregateSerde.deserializer()
        )
    }

    private fun seedEmptyGarage(garage: ParkingGarage) {

        garageTopic.pipeInput(garage.id, garage)

        for (z in garage.parkingZonesList) {
            for (r in z.parkingRowsList) {
                for (s in r.parkingSpacesList) {
                    val spaceStatus = ParkingSpaceStatus.newBuilder()
                        .setId(s.id)
                        .setStatus(SpaceStatus.VACANT)
                        .setSpace(s)
                        .setLastUpdated(Timestamp.newBuilder().build())
                        .build()

                    spaceStatusTopic.pipeInput(spaceStatus.id, spaceStatus)
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(VehicleType::class, names = ["UNRECOGNIZED"], mode = EnumSource.Mode.EXCLUDE)
    fun `test for row changes`(vehicleType: VehicleType) {
        // Find a space of the specified vehicle type
        val space = testGarage.parkingZonesList
            .flatMap { it.parkingRowsList }
            .flatMap { it.parkingSpacesList }
            .first { it.type == vehicleType }
    
        // Create a space status update for the space
        val spaceStatus = ParkingSpaceStatus.newBuilder()
            .setId(space.id)
            .setStatus(SpaceStatus.OCCUPIED) // Change to OCCUPIED
            .setSpace(space)
            .setLastUpdated(Timestamp.newBuilder().build())
            .build()
    
        // Send the space status update to the topic
        spaceStatusTopic.pipeInput(spaceStatus.id, spaceStatus)
    
        // Get the row ID
        val rowId = "${space.garageId}-${space.zoneId}-${space.rowId}"
    
        // Get the output from the topic
        val outputRecords = outputTopic.readRecordsToList()
        // Verify that we got an output record
        assert(outputRecords.isNotEmpty()) { "No output records produced" }
        // Get the last record (should be the most recent update)
        val lastRecord = outputRecords.last()
        // Verify the key is the row ID
        assert(lastRecord.key == rowId) { "Expected key to be $rowId but was ${lastRecord.key}" }
    
        // Get the row status
        val rowStatus = lastRecord.value
        // Verify the row status has the correct ID
        assert(rowStatus.id == rowId) { "Expected row status ID to be $rowId but was ${rowStatus.id}" }
    
        // Verify the capacities on the output match that of the garage/zone/row that was joined.
        assertEquals(defaultRowCapacity, rowStatus.defaultStatus.capacity,
            "Expected default capacity to be $defaultRowCapacity but was ${rowStatus.defaultStatus.capacity}")
        assertEquals(handicapRowCapacity, rowStatus.handicapStatus.capacity,
            "Expected handicap capacity to be $handicapRowCapacity but was ${rowStatus.handicapStatus.capacity}")
        assertEquals(motorcycleRowCapacity, rowStatus.motorcycleStatus.capacity,
            "Expected motorcycle capacity to be $motorcycleRowCapacity but was ${rowStatus.motorcycleStatus.capacity}")
    
        // Verify the occupied count is 1 for the type of the space we updated
        // and 0 for the other types
        val expectedDefaultOccupied = if (vehicleType == VehicleType.DEFAULT) 1 else 0
        val expectedHandicapOccupied = if (vehicleType == VehicleType.HANDICAP) 1 else 0
        val expectedMotorcycleOccupied = if (vehicleType == VehicleType.MOTORCYCLE) 1 else 0
        
        assert(rowStatus.defaultStatus.occupied == expectedDefaultOccupied) { 
            "Expected default occupied to be $expectedDefaultOccupied but was ${rowStatus.defaultStatus.occupied}" 
        }
        assert(rowStatus.handicapStatus.occupied == expectedHandicapOccupied) { 
            "Expected handicap occupied to be $expectedHandicapOccupied but was ${rowStatus.handicapStatus.occupied}" 
        }
        assert(rowStatus.motorcycleStatus.occupied == expectedMotorcycleOccupied) { 
            "Expected motorcycle occupied to be $expectedMotorcycleOccupied but was ${rowStatus.motorcycleStatus.occupied}" 
        }
    
        // Now change the space back to VACANT
        val vacantSpaceStatus = ParkingSpaceStatus.newBuilder()
            .setId(space.id)
            .setStatus(SpaceStatus.VACANT)
            .setSpace(space)
            .setLastUpdated(Timestamp.newBuilder().build())
            .build()
    
        // Send the space status update to the topic
        spaceStatusTopic.pipeInput(vacantSpaceStatus.id, vacantSpaceStatus)
    
        // Get the output from the topic
        val vacantOutputRecords = outputTopic.readRecordsToList()
    
        // Verify that we got an output record
        assert(vacantOutputRecords.isNotEmpty()) { "No output records produced for VACANT update" }
    
        // Get the last record (should be the most recent update)
        val vacantLastRecord = vacantOutputRecords.last()
    
        // Get the row status
        val vacantRowStatus = vacantLastRecord.value
    
        // Verify the occupied count is back to 0 for the type of the space we updated
        assert(vacantRowStatus.defaultStatus.occupied == 0) { 
            "Expected default occupied to be 0 but was ${vacantRowStatus.defaultStatus.occupied}" 
        }
        assert(vacantRowStatus.handicapStatus.occupied == 0) { 
            "Expected handicap occupied to be 0 but was ${vacantRowStatus.handicapStatus.occupied}" 
        }
        assert(vacantRowStatus.motorcycleStatus.occupied == 0) { 
            "Expected motorcycle occupied to be 0 but was ${vacantRowStatus.motorcycleStatus.occupied}" 
        }
    }

}