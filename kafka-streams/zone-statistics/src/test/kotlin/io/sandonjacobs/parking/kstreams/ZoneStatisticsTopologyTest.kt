package io.sandonjacobs.parking.kstreams

import com.google.protobuf.Timestamp
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.sandonjacobs.parking.kstreams.faker.GarageFaker
import io.sandonjacobs.parking.kstreams.serde.SerdeProvider
import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.Vehicle
import io.sandonjacobs.streaming.parking.model.VehicleType
import io.sandonjacobs.streaming.parking.status.ParkingGarageZoneStatus
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import io.sandonjacobs.streaming.parking.status.SpaceStatus
import io.sandonjacobs.streaming.parking.status.ZoneStatus
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.lang.Thread.sleep
import kotlin.test.assertEquals

class ZoneStatisticsTopologyTest {

    @ParameterizedTest
    @EnumSource(VehicleType::class, names = ["UNRECOGNIZED"], mode = EnumSource.Mode.EXCLUDE)
    fun `checks for zone occupancy accumulation`(vehicleType: VehicleType) {
        // Choose the first zone
        val zone = testGarage.parkingZonesList.first()
        val spaces = zone.parkingRowsList.flatMap { it.parkingSpacesList }.filter { it.type == vehicleType }

        // Send OCCUPIED for each space of the given type in the zone
        spaces.forEach { space ->
            val status = ParkingSpaceStatus.newBuilder()
                .setId(space.id)
                .setStatus(SpaceStatus.OCCUPIED)
                .setSpace(space)
                .setLastUpdated(Timestamp.newBuilder().build())
                .setVehicle(Vehicle.newBuilder().build())
                .build()
            spaceStatusTopic.pipeInput(status.id, status)
        }

        val outputRecords = outputTopic.readRecordsToList()
        assert(outputRecords.isNotEmpty()) { "No output records produced" }
        val lastRecord = outputRecords.last().value()

        when (vehicleType) {
            VehicleType.CAR -> assertEquals(spaces.size, lastRecord.carStatus.occupied)
            VehicleType.MOTORCYCLE -> assertEquals(spaces.size, lastRecord.motorcycleStatus.occupied)
            VehicleType.HANDICAP -> assertEquals(spaces.size, lastRecord.handicapStatus.occupied)
            VehicleType.UNRECOGNIZED -> kotlin.test.fail("Unexpected UNRECOGNIZED type")
        }
    }

    private lateinit var topology: ZoneStatisticsTopology
    private lateinit var garageFaker: GarageFaker

    private lateinit var testDriver: TopologyTestDriver
    private lateinit var spaceStatusTopic: TestInputTopic<String, ParkingSpaceStatus>
    private lateinit var garageTopic: TestInputTopic<String, ParkingGarage>

    private lateinit var outputTopic: TestOutputTopic<String, ParkingGarageZoneStatus>

    private lateinit var parkingSpaceStatusSerde: Serde<ParkingSpaceStatus>
    private lateinit var garageSerde: Serde<ParkingGarage>
    private lateinit var zoneStatisticsSerde: Serde<ParkingGarageZoneStatus>

    private lateinit var testGarage: ParkingGarage

    // These values match the hardcoded values in ZoneCalculator
    private val carRowCapacity = 20
    private val handicapRowCapacity = 4
    private val motorcycleRowCapacity = 1
    private val rowsPerZone = 2

    @BeforeEach
    fun setup() {
        val serdeProps = mapOf(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://")

        parkingSpaceStatusSerde = SerdeProvider.createProtobufSerde(ParkingSpaceStatus::class, serdeProps)
        garageSerde = SerdeProvider.createProtobufSerde(ParkingGarage::class, serdeProps)
        zoneStatisticsSerde = SerdeProvider.createProtobufSerde(ParkingGarageZoneStatus::class, serdeProps)

        // Initialize the faker
        garageFaker = GarageFaker()
        testGarage = garageFaker.createMockGarage("g001", 2, rowsPerZone,
            carRowCapacity, handicapRowCapacity, motorcycleRowCapacity)

        topology = ZoneStatisticsTopology(parkingSpaceStatusSerde, garageSerde, zoneStatisticsSerde)
        val builder = StreamsBuilder()

        testDriver = TopologyTestDriver(topology.buildTopology(builder))

        spaceStatusTopic = testDriver.createInputTopic(ZoneStatisticsTopology.PARKING_SPACE_STATUS_TOPIC,
            Serdes.String().serializer(), parkingSpaceStatusSerde.serializer())

        // Add a topic for the parking garage data
        garageTopic = testDriver.createInputTopic(ZoneStatisticsTopology.PARKING_GARAGE_TOPIC,
            Serdes.String().serializer(), garageSerde.serializer())

        initGarage(testGarage)

        outputTopic = testDriver.createOutputTopic(ZoneStatisticsTopology.ZONE_STATISTICS_TOPIC,
            Serdes.String().deserializer(), zoneStatisticsSerde.deserializer()
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
    fun `test for zone changes`(vehicleType: VehicleType) {
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
    
        // Get the zone ID
        val zoneId = "${space.garageId}-${space.zoneId}"
    
        // Get the output from the topic
        val outputRecords = outputTopic.readRecordsToList()
        // Verify that we got an output record
        assert(outputRecords.isNotEmpty()) { "No output records produced" }
        // Get the last record (should be the most recent update)
        val lastRecord = outputRecords.last()
        // Verify the key is the zone ID
        assert(lastRecord.key == zoneId) { "Expected key to be $zoneId but was ${lastRecord.key}" }
    
        // Get the zone status
        val zoneStatus = lastRecord.value
        // Verify the zone status has the correct ID
        assert(zoneStatus.id == zoneId) { "Expected zone status ID to be $zoneId but was ${zoneStatus.id}" }
    
        // Expected capacities are multiplied by the number of rows per zone
        val expectedCarCapacity = carRowCapacity * rowsPerZone
        val expectedHandicapCapacity = handicapRowCapacity * rowsPerZone
        val expectedMotorcycleCapacity = motorcycleRowCapacity * rowsPerZone

        // Verify the capacities on the output match that of the garage/zone that was joined.
        assertEquals(expectedCarCapacity, zoneStatus.carStatus.capacity,
            "Expected car capacity to be $expectedCarCapacity but was ${zoneStatus.carStatus.capacity}")
        assertEquals(expectedHandicapCapacity, zoneStatus.handicapStatus.capacity,
            "Expected handicap capacity to be $expectedHandicapCapacity but was ${zoneStatus.handicapStatus.capacity}")
        assertEquals(expectedMotorcycleCapacity, zoneStatus.motorcycleStatus.capacity,
            "Expected motorcycle capacity to be $expectedMotorcycleCapacity but was ${zoneStatus.motorcycleStatus.capacity}")
    
        // Verify the occupied count is 1 for the type of the space we updated
        // and 0 for the other types
        val expectedDefaultOccupied = if (vehicleType == VehicleType.CAR) 1 else 0
        val expectedHandicapOccupied = if (vehicleType == VehicleType.HANDICAP) 1 else 0
        val expectedMotorcycleOccupied = if (vehicleType == VehicleType.MOTORCYCLE) 1 else 0
        
        assert(zoneStatus.carStatus.occupied == expectedDefaultOccupied) { 
            "Expected car occupied to be $expectedDefaultOccupied but was ${zoneStatus.carStatus.occupied}" 
        }
        assert(zoneStatus.handicapStatus.occupied == expectedHandicapOccupied) { 
            "Expected handicap occupied to be $expectedHandicapOccupied but was ${zoneStatus.handicapStatus.occupied}" 
        }
        assert(zoneStatus.motorcycleStatus.occupied == expectedMotorcycleOccupied) { 
            "Expected motorcycle occupied to be $expectedMotorcycleOccupied but was ${zoneStatus.motorcycleStatus.occupied}" 
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
    
        // Get the zone status
        val vacantZoneStatus = vacantLastRecord.value
    
        // Verify the occupied count is back to 0 for the type of the space we updated
        assert(vacantZoneStatus.carStatus.occupied == 0) { 
            "Expected car occupied to be 0 but was ${vacantZoneStatus.carStatus.occupied}" 
        }
        assert(vacantZoneStatus.handicapStatus.occupied == 0) { 
            "Expected handicap occupied to be 0 but was ${vacantZoneStatus.handicapStatus.occupied}" 
        }
        assert(vacantZoneStatus.motorcycleStatus.occupied == 0) { 
            "Expected motorcycle occupied to be 0 but was ${vacantZoneStatus.motorcycleStatus.occupied}" 
        }
    }

    @ParameterizedTest
    @EnumSource(VehicleType::class, names = ["UNRECOGNIZED"], mode = EnumSource.Mode.EXCLUDE)
    fun `update zone status for OCCUPIED status for any vehicle type`(vehicleType: VehicleType) {

        val randomParkingSpace = testGarage.parkingZonesList.random().parkingRowsList.random().parkingSpacesList.first { it.type == vehicleType }
        val currentZoneStatus = ParkingGarageZoneStatus.newBuilder()
            .setId("${randomParkingSpace.garageId}-${randomParkingSpace.zoneId}")
            .setZoneId(randomParkingSpace.zoneId)
            .setGarageId(randomParkingSpace.garageId)
            .setCarStatus(ZoneStatus.newBuilder()
                .setCapacity(5)
                .setOccupied(1)
                .setVehicleType(VehicleType.CAR)
                .build())
            .setHandicapStatus(ZoneStatus.newBuilder()
                .setCapacity(1)
                .setOccupied(0)
                .setVehicleType(VehicleType.HANDICAP)
                .build())
            .setMotorcycleStatus(ZoneStatus.newBuilder()
                .setCapacity(1)
                .setOccupied(1)
                .setVehicleType(VehicleType.MOTORCYCLE)
                .build())
            .build()

        val spaceStatus = ParkingSpaceStatus.newBuilder()
            .setId(randomParkingSpace.id)
            .setStatus(SpaceStatus.OCCUPIED) // Change to OCCUPIED
            .setSpace(randomParkingSpace)
            .setLastUpdated(Timestamp.newBuilder().build())
            .build()

        val result = topology.updateZoneStatus(spaceStatus, currentZoneStatus, testGarage)
        assertNotNull(result)

        // Expected capacities are multiplied by the number of rows per zone
        val expectedCarCapacity = carRowCapacity * rowsPerZone
        val expectedHandicapCapacity = handicapRowCapacity * rowsPerZone
        val expectedMotorcycleCapacity = motorcycleRowCapacity * rowsPerZone

        // Verify the capacities on the output match that of the garage/zone that was joined.
        assertEquals(expectedCarCapacity, result.carStatus.capacity,
            "Expected car capacity to be $expectedCarCapacity but was ${result.carStatus.capacity}")
        assertEquals(expectedHandicapCapacity, result.handicapStatus.capacity,
            "Expected handicap capacity to be $expectedHandicapCapacity but was ${result.handicapStatus.capacity}")
        assertEquals(expectedMotorcycleCapacity, result.motorcycleStatus.capacity,
            "Expected motorcycle capacity to be $expectedMotorcycleCapacity but was ${result.motorcycleStatus.capacity}")
    }

    @ParameterizedTest
    @EnumSource(VehicleType::class, names = ["UNRECOGNIZED"], mode = EnumSource.Mode.EXCLUDE)
    fun `update zone status for VACANT status for any vehicle type`(vehicleType: VehicleType) {

        val randomParkingSpace = testGarage.parkingZonesList.random().parkingRowsList.random().parkingSpacesList.first { it.type == vehicleType }
        val currentZoneStatus = ParkingGarageZoneStatus.newBuilder()
            .setId("${randomParkingSpace.garageId}-${randomParkingSpace.zoneId}")
            .setZoneId(randomParkingSpace.zoneId)
            .setGarageId(randomParkingSpace.garageId)
            .setCarStatus(ZoneStatus.newBuilder()
                .setCapacity(5)
                .setOccupied(1)
                .setVehicleType(VehicleType.CAR)
                .build())
            .setHandicapStatus(ZoneStatus.newBuilder()
                .setCapacity(1)
                .setOccupied(0)
                .setVehicleType(VehicleType.HANDICAP)
                .build())
            .setMotorcycleStatus(ZoneStatus.newBuilder()
                .setCapacity(1)
                .setOccupied(1)
                .setVehicleType(VehicleType.MOTORCYCLE)
                .build())
            .build()

        val spaceStatus = ParkingSpaceStatus.newBuilder()
            .setId(randomParkingSpace.id)
            .setStatus(SpaceStatus.VACANT) // Change to VACANT
            .setSpace(randomParkingSpace)
            .setLastUpdated(Timestamp.newBuilder().build())
            .build()

        val result = topology.updateZoneStatus(spaceStatus, currentZoneStatus, testGarage)
        assertNotNull(result)

        // Expected capacities are multiplied by the number of rows per zone
        val expectedCarCapacity = carRowCapacity * rowsPerZone
        val expectedHandicapCapacity = handicapRowCapacity * rowsPerZone
        val expectedMotorcycleCapacity = motorcycleRowCapacity * rowsPerZone

        // Verify the capacities on the output match that of the garage/zone that was joined.
        assertEquals(expectedCarCapacity, result.carStatus.capacity,
            "Expected car capacity to be $expectedCarCapacity but was ${result.carStatus.capacity}")
        assertEquals(expectedHandicapCapacity, result.handicapStatus.capacity,
            "Expected handicap capacity to be $expectedHandicapCapacity but was ${result.handicapStatus.capacity}")
        assertEquals(expectedMotorcycleCapacity, result.motorcycleStatus.capacity,
            "Expected motorcycle capacity to be $expectedMotorcycleCapacity but was ${result.motorcycleStatus.capacity}")
    }

    @ParameterizedTest
    @EnumSource(VehicleType::class, names = ["UNRECOGNIZED"], mode = EnumSource.Mode.EXCLUDE)
    fun `row occupancy should never exceed the capacity`(vehicleType: VehicleType) {
        initGarage(testGarage, SpaceStatus.OCCUPIED)

        sleep(1500)
        val randomZone = testGarage.parkingZonesList.random()

        val randomRow = randomZone.parkingRowsList.random()

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

        val expectedOccupancy = randomZone.parkingRowsList.flatMap {it.parkingSpacesList}.filter { it.type == vehicleType }.size
        when (vehicleType) {
            VehicleType.CAR -> { assertEquals(expectedOccupancy, lastRecord.carStatus.occupied) }
            VehicleType.MOTORCYCLE -> { assertEquals(expectedOccupancy, lastRecord.motorcycleStatus.occupied) }
            VehicleType.HANDICAP -> { assertEquals(expectedOccupancy, lastRecord.handicapStatus.occupied) }
            VehicleType.UNRECOGNIZED -> fail { "What is a $vehicleType?" }
        }
    }
}