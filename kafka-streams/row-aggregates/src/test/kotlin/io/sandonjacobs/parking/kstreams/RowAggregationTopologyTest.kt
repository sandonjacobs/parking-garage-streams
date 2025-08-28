package io.sandonjacobs.parking.kstreams

import com.google.protobuf.Timestamp
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.sandonjacobs.parking.kstreams.faker.GarageFaker
import io.sandonjacobs.parking.kstreams.serde.SerdeProvider
import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.Vehicle
import io.sandonjacobs.streaming.parking.model.VehicleType
import io.sandonjacobs.streaming.parking.status.ParkingGarageRowStatus
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import io.sandonjacobs.streaming.parking.status.RowStatus
import io.sandonjacobs.streaming.parking.status.SpaceStatus
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.lang.Thread.sleep
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
    private val carRowCapacity = 20
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
            carRowCapacity, handicapRowCapacity, motorcycleRowCapacity)

        topology = RowAggregationTopology(parkingSpaceStatusSerde, garageSerde, rowAggregateSerde)
        val builder = StreamsBuilder()

        testDriver = TopologyTestDriver(topology.buildTopology(builder))

        spaceStatusTopic = testDriver.createInputTopic(RowAggregationTopology.PARKING_SPACE_STATUS_TOPIC,
            Serdes.String().serializer(), parkingSpaceStatusSerde.serializer())

        // Add a topic for the parking garage data
        garageTopic = testDriver.createInputTopic(RowAggregationTopology.PARKING_GARAGE_TOPIC,
            Serdes.String().serializer(), garageSerde.serializer())

        initGarage(testGarage)

        outputTopic = testDriver.createOutputTopic(RowAggregationTopology.ROW_AGGREGATION_TOPIC,
            Serdes.String().deserializer(), rowAggregateSerde.deserializer()
        )
    }

    private fun initGarage(garage: ParkingGarage, statusForAll: SpaceStatus = SpaceStatus.VACANT) {

        garageTopic.pipeInput(garage.id, garage)

        for (z in garage.parkingZonesList) {
            for (r in z.parkingRowsList) {
                for (s in r.parkingSpacesList) {
                    val spaceStatus = ParkingSpaceStatus.newBuilder()
                        .setId(s.id)
                        .setStatus(statusForAll)
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
    fun `checks for occupancy changes`(vehicleType: VehicleType) {
        val randomRow = testGarage.parkingZonesList
            .flatMap { it.parkingRowsList }
            .first()

        val spaces = randomRow.parkingSpacesList.filter { it.type == vehicleType }

        spaces.forEach { it ->

                val status = ParkingSpaceStatus.newBuilder()
                    .setId(it.id)
                    .setStatus(SpaceStatus.OCCUPIED) // Change to OCCUPIED
                    .setSpace(it)
                    .setLastUpdated(Timestamp.newBuilder().build())
                    .setVehicle(Vehicle.newBuilder().build())
                    .build()

                spaceStatusTopic.pipeInput(status.id, status)

        }
        val outputRecords = outputTopic.readRecordsToList()
        assert(outputRecords.isNotEmpty()) { "No output records produced" }
        val lastRecord = outputRecords.last().value()

        when (vehicleType) {
            VehicleType.CAR -> { assertEquals(spaces.size, lastRecord.carStatus.occupied) }
            VehicleType.MOTORCYCLE -> { assertEquals(spaces.size, lastRecord.motorcycleStatus.occupied) }
            VehicleType.HANDICAP -> { assertEquals(spaces.size, lastRecord.handicapStatus.occupied) }
            VehicleType.UNRECOGNIZED -> fail { "What is a $vehicleType?" }
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
        assertEquals(carRowCapacity, rowStatus.carStatus.capacity,
            "Expected default capacity to be $carRowCapacity but was ${rowStatus.carStatus.capacity}")
        assertEquals(handicapRowCapacity, rowStatus.handicapStatus.capacity,
            "Expected handicap capacity to be $handicapRowCapacity but was ${rowStatus.handicapStatus.capacity}")
        assertEquals(motorcycleRowCapacity, rowStatus.motorcycleStatus.capacity,
            "Expected motorcycle capacity to be $motorcycleRowCapacity but was ${rowStatus.motorcycleStatus.capacity}")

        // Verify the occupied count is 1 for the type of the space we updated
        // and 0 for the other types
        val expectedDefaultOccupied = if (vehicleType == VehicleType.CAR) 1 else 0
        val expectedHandicapOccupied = if (vehicleType == VehicleType.HANDICAP) 1 else 0
        val expectedMotorcycleOccupied = if (vehicleType == VehicleType.MOTORCYCLE) 1 else 0

        assert(rowStatus.carStatus.occupied == expectedDefaultOccupied) {
            "Expected default occupied to be $expectedDefaultOccupied but was ${rowStatus.carStatus.occupied}"
        }
        assert(rowStatus.handicapStatus.occupied == expectedHandicapOccupied) {
            "Expected handicap occupied to be $expectedHandicapOccupied but was ${rowStatus.handicapStatus.occupied}"
        }
        assert(rowStatus.motorcycleStatus.occupied == expectedMotorcycleOccupied) {
            "Expected motorcycle occupied to be $expectedMotorcycleOccupied but was ${rowStatus.motorcycleStatus.occupied}"
        }

        // Now change the space back to VACANT
        val vacantSpaceStatus = ParkingSpaceStatus.newBuilder(spaceStatus)
            .setStatus(SpaceStatus.VACANT)
            .setLastUpdated(Timestamp.newBuilder().build())
            .setVehicle(Vehicle.getDefaultInstance())
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
        assert(vacantRowStatus.carStatus.occupied == 0) {
            "Expected default occupied to be 0 but was ${vacantRowStatus.carStatus.occupied}"
        }
        assert(vacantRowStatus.handicapStatus.occupied == 0) {
            "Expected handicap occupied to be 0 but was ${vacantRowStatus.handicapStatus.occupied}"
        }
        assert(vacantRowStatus.motorcycleStatus.occupied == 0) {
            "Expected motorcycle occupied to be 0 but was ${vacantRowStatus.motorcycleStatus.occupied}"
        }
    }

    @ParameterizedTest
    @EnumSource(VehicleType::class, names = ["UNRECOGNIZED"], mode = EnumSource.Mode.EXCLUDE)
    fun `update row status for OCCUPIED status for any vehicle type`(vehicleType: VehicleType) {

        val randomParkingSpace = testGarage.parkingZonesList.random().parkingRowsList.random().parkingSpacesList.first { it.type == vehicleType }
        val currentRowStatus = ParkingGarageRowStatus.newBuilder()
            .setId(randomParkingSpace.rowId)
            .setCarStatus(RowStatus.newBuilder()
                .setCapacity(carRowCapacity)
                .setOccupied(1)
                .setVehicleType(VehicleType.CAR)
                .build())
            .setHandicapStatus(RowStatus.newBuilder()
                .setCapacity(handicapRowCapacity)
                .setOccupied(0)
                .setVehicleType(VehicleType.HANDICAP)
                .build())
            .setMotorcycleStatus(RowStatus.newBuilder()
                .setCapacity(motorcycleRowCapacity)
                .setOccupied(0)
                .setVehicleType(VehicleType.MOTORCYCLE)
                .build())
            .build()

        val spaceStatus = ParkingSpaceStatus.newBuilder()
            .setId(randomParkingSpace.id)
            .setStatus(SpaceStatus.OCCUPIED) // Change to OCCUPIED
            .setSpace(randomParkingSpace)
            .setLastUpdated(Timestamp.newBuilder().build())
            .build()

        val result = topology.updateRowStatus(spaceStatus, currentRowStatus, testGarage)
        assertNotNull(result)

        // Verify the capacities on the output match that of the garage/zone/row that was joined.
        assertEquals(carRowCapacity, result.carStatus.capacity,
            "Expected default capacity to be $carRowCapacity but was ${result.carStatus.capacity}")
        assertEquals(handicapRowCapacity, result.handicapStatus.capacity,
            "Expected handicap capacity to be $handicapRowCapacity but was ${result.handicapStatus.capacity}")
        assertEquals(motorcycleRowCapacity, result.motorcycleStatus.capacity,
            "Expected motorcycle capacity to be $motorcycleRowCapacity but was ${result.motorcycleStatus.capacity}")


        when (vehicleType) {
            VehicleType.CAR -> { assertEquals(2, result.carStatus.occupied) }
            VehicleType.MOTORCYCLE -> { assertEquals(1, result.motorcycleStatus.occupied) }
            VehicleType.HANDICAP -> { assertEquals(1, result.handicapStatus.occupied) }
            VehicleType.UNRECOGNIZED -> fail { "What is a $vehicleType?" }
        }
    }

    @ParameterizedTest
    @EnumSource(VehicleType::class, names = ["UNRECOGNIZED"], mode = EnumSource.Mode.EXCLUDE)
    fun `update row status for VACANT status for any vehicle type`(vehicleType: VehicleType) {

        val randomParkingSpace = testGarage.parkingZonesList.random().parkingRowsList.random().parkingSpacesList.first { it.type == vehicleType }
        val currentRowStatus = ParkingGarageRowStatus.newBuilder()
            .setId(randomParkingSpace.rowId)
            .setCarStatus(RowStatus.newBuilder()
                .setCapacity(5)
                .setOccupied(1)
                .setVehicleType(VehicleType.CAR)
                .build())
            .setHandicapStatus(RowStatus.newBuilder()
                .setCapacity(1)
                .setOccupied(0)
                .setVehicleType(VehicleType.HANDICAP)
                .build())
            .setMotorcycleStatus(RowStatus.newBuilder()
                .setCapacity(1)
                .setOccupied(1)
                .setVehicleType(VehicleType.MOTORCYCLE)
                .build())
            .build()

        val spaceStatus = ParkingSpaceStatus.newBuilder()
            .setId(randomParkingSpace.id)
            .setStatus(SpaceStatus.VACANT) // Change to OCCUPIED
            .setSpace(randomParkingSpace)
            .setLastUpdated(Timestamp.newBuilder().build())
            .build()

        val result = topology.updateRowStatus(spaceStatus, currentRowStatus, testGarage)
        assertNotNull(result)

        // Verify the capacities on the output match that of the garage/zone/row that was joined.
        assertEquals(carRowCapacity, result.carStatus.capacity,
            "Expected default capacity to be $carRowCapacity but was ${result.carStatus.capacity}")
        assertEquals(handicapRowCapacity, result.handicapStatus.capacity,
            "Expected handicap capacity to be $handicapRowCapacity but was ${result.handicapStatus.capacity}")
        assertEquals(motorcycleRowCapacity, result.motorcycleStatus.capacity,
            "Expected motorcycle capacity to be $motorcycleRowCapacity but was ${result.motorcycleStatus.capacity}")

    }

    @ParameterizedTest
    @EnumSource(VehicleType::class, names = ["UNRECOGNIZED"], mode = EnumSource.Mode.EXCLUDE)
    fun `row occupancy should never exceed the capacity`(vehicleType: VehicleType) {
        initGarage(testGarage, SpaceStatus.OCCUPIED)

        sleep(1500)
        val randomRow = testGarage.parkingZonesList
            .flatMap { it.parkingRowsList }
            .first()

        // get a random space
        val randomSpace = randomRow.parkingSpacesList.filter { it.type == vehicleType }.random()
        // set its status to OCCUPIED
        val status = ParkingSpaceStatus.newBuilder()
                .setId(randomSpace.id)
                .setStatus(SpaceStatus.OCCUPIED) // Change to OCCUPIED
                .setSpace(randomSpace)
                .setLastUpdated(Timestamp.newBuilder().build())
                .setVehicle(Vehicle.newBuilder()
                    .setType(vehicleType)
                    .build())
                .build()

        spaceStatusTopic.pipeInput(status.id, status)

        val outputRecords = outputTopic.readRecordsToList()
        assert(outputRecords.isNotEmpty()) { "No output records produced" }
        val lastRecord = outputRecords.last().value()

        val expectedOccupancy = randomRow.parkingSpacesList.filter { it.type == vehicleType }.size
        when (vehicleType) {
            VehicleType.CAR -> { assertEquals(expectedOccupancy, lastRecord.carStatus.occupied) }
            VehicleType.MOTORCYCLE -> { assertEquals(expectedOccupancy, lastRecord.motorcycleStatus.occupied) }
            VehicleType.HANDICAP -> { assertEquals(expectedOccupancy, lastRecord.handicapStatus.occupied) }
            VehicleType.UNRECOGNIZED -> fail { "What is a $vehicleType?" }
        }
    }
}