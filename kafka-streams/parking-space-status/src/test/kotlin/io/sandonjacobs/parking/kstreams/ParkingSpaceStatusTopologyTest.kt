package io.sandonjacobs.parking.kstreams

import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde
import io.sandonjacobs.streaming.parking.model.*
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import io.sandonjacobs.streaming.parking.status.SpaceStatus
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertFalse

class ParkingSpaceStatusTopologyTest {

    private lateinit var topology: ParkingSpaceStatusTopology
    private lateinit var parkingEventSerde: Serde<ParkingEvent>
    private lateinit var parkingSpaceStatusSerde: Serde<ParkingSpaceStatus>

    private lateinit var testDriver: TopologyTestDriver
    private lateinit var inputTopic: TestInputTopic<String, ParkingEvent>
    private lateinit var outputTopic: TestOutputTopic<String, ParkingSpaceStatus>

    @BeforeEach
    fun setUp() {
        // Create test serdes
        parkingEventSerde = createParkingEventSerde()
        parkingSpaceStatusSerde = createParkingSpaceStatusSerde()

        // Create topology
        topology = ParkingSpaceStatusTopology(parkingEventSerde, parkingSpaceStatusSerde)

        val builder = StreamsBuilder()
        testDriver = TopologyTestDriver(topology.buildTopology(builder))

        // Create test topics
        inputTopic = testDriver.createInputTopic(
            ParkingSpaceStatusTopology.PARKING_EVENTS_TOPIC,
            Serdes.String().serializer(),
            parkingEventSerde.serializer()
        )

        outputTopic = testDriver.createOutputTopic(
            ParkingSpaceStatusTopology.PARKING_SPACE_STATUS_TOPIC,
            Serdes.String().deserializer(),
            parkingSpaceStatusSerde.deserializer()
        )
    }

    private fun createParkingEventSerde(): Serde<ParkingEvent> {
        val serde = KafkaProtobufSerde<ParkingEvent>()
        serde.configure(
            mapOf(
                "schema.registry.url" to "mock://test",
                "specific.protobuf.value.type" to ParkingEvent::class.java.name
            ),
            false
        )
        return serde
    }

    private fun createParkingSpaceStatusSerde(): Serde<ParkingSpaceStatus> {
        val serde = KafkaProtobufSerde<ParkingSpaceStatus>()
        serde.configure(
            mapOf(
                "schema.registry.url" to "mock://test",
                "specific.protobuf.value.type" to ParkingSpaceStatus::class.java.name
            ),
            false
        )
        return serde
    }

    @AfterEach
    fun tearDown() {
        testDriver.close()
    }

    @Test
    fun `should process ENTER event and output OCCUPIED status`() {
        // Given
        val parkingSpace = createTestParkingSpace("space-1", "zone-1", "garage-1")
        val vehicle = createTestVehicle("vehicle-1", "ABC123", "CA", VehicleType.CAR)
        val enterEvent = createParkingEvent(ParkingEventType.ENTER, parkingSpace, vehicle)

        // When
        inputTopic.pipeInput(parkingSpace.id, enterEvent)

        // Then
        val outputRecords = outputTopic.readRecordsToList()
        assertEquals(1, outputRecords.size)

        val statusRecord = outputRecords[0]
        assertEquals(parkingSpace.id, statusRecord.key)

        val status = statusRecord.value
        assertEquals(parkingSpace.id, status.id)
        assertEquals(parkingSpace, status.space)
        assertEquals(SpaceStatus.OCCUPIED, status.status)
        assertEquals(vehicle, status.vehicle)
        assertNotNull(status.lastUpdated)
    }

    @Test
    fun `process EXIT events`() {
        // Given
        val parkingSpace = createTestParkingSpace("space-1", "zone-1", "garage-1")
        val vehicle = createTestVehicle("vehicle-1", "ABC123", "CA", VehicleType.CAR)
        val exitEvent = createParkingEvent(ParkingEventType.EXIT, parkingSpace, vehicle)

        // When
        inputTopic.pipeInput(parkingSpace.id, exitEvent)

        // Then
        val outputRecords = outputTopic.readRecordsToList()
        assertEquals(1, outputRecords.size)
        val vacantEvent = outputRecords[0].value
        assertEquals(parkingSpace.id, vacantEvent.id)
        assertEquals(SpaceStatus.VACANT, vacantEvent.status)
        assertFalse(vacantEvent.hasVehicle())
    }

    @Test
    fun `should process multiple ENTER events for different spaces`() {
        // Given
        val space1 = createTestParkingSpace("space-1", "zone-1", "garage-1")
        val space2 = createTestParkingSpace("space-2", "zone-1", "garage-1")
        val vehicle1 = createTestVehicle("vehicle-1", "ABC123", "CA", VehicleType.CAR)
        val vehicle2 = createTestVehicle("vehicle-2", "XYZ789", "NY", VehicleType.HANDICAP)

        val enterEvent1 = createParkingEvent(ParkingEventType.ENTER, space1, vehicle1)
        val enterEvent2 = createParkingEvent(ParkingEventType.ENTER, space2, vehicle2)

        // When
        inputTopic.pipeInput(space1.id, enterEvent1)
        inputTopic.pipeInput(space2.id, enterEvent2)

        // Then
        val outputRecords = outputTopic.readRecordsToList()
        assertEquals(2, outputRecords.size)

        // Verify first record
        val status1 = outputRecords[0].value
        assertEquals(space1.id, status1.id)
        assertEquals(SpaceStatus.OCCUPIED, status1.status)
        assertEquals(vehicle1, status1.vehicle)

        // Verify second record
        val status2 = outputRecords[1].value
        assertEquals(space2.id, status2.id)
        assertEquals(SpaceStatus.OCCUPIED, status2.status)
        assertEquals(vehicle2, status2.vehicle)
    }

    @Test
    fun `should handle mixed ENTER and EXIT events correctly`() {
        // Given
        val parkingSpace = createTestParkingSpace("space-1", "zone-1", "garage-1")
        val vehicle = createTestVehicle("vehicle-1", "ABC123", "CA", VehicleType.CAR)

        val enterEvent = createParkingEvent(ParkingEventType.ENTER, parkingSpace, vehicle)
        val exitEvent = createParkingEvent(ParkingEventType.EXIT, parkingSpace, vehicle)

        // When
        inputTopic.pipeInput(parkingSpace.id, enterEvent)
        inputTopic.pipeInput(parkingSpace.id, exitEvent)

        // Then
        val outputRecords = outputTopic.readRecordsToList()
        assertEquals(2, outputRecords.size)

        val outputValues = outputRecords.map { it.value }
        assertNotNull(outputValues.find { ps -> ps.id.equals(parkingSpace.id) && ps.status.equals(SpaceStatus.OCCUPIED) })
        assertNotNull(outputValues.find { ps -> ps.id.equals(parkingSpace.id) && ps.status.equals(SpaceStatus.VACANT) })
    }

    // Helper functions to create test data
    private fun createTestParkingSpace(id: String, zoneId: String, garageId: String): ParkingSpace {
        return ParkingSpace.newBuilder()
            .setId(id)
            .setZoneId(zoneId)
            .setGarageId(garageId)
            .setType(VehicleType.CAR)
            .build()
    }

    private fun createTestVehicle(id: String, licensePlate: String, state: String, type: VehicleType): Vehicle {
        return Vehicle.newBuilder()
            .setId(id)
            .setLicensePlate(licensePlate)
            .setState(state)
            .setType(type)
            .build()
    }

    private fun createParkingEvent(type: ParkingEventType, space: ParkingSpace, vehicle: Vehicle): ParkingEvent {
        val now = Instant.now()
        return ParkingEvent.newBuilder()
            .setType(type)
            .setSpace(space)
            .setVehicle(vehicle)
            .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(now.epochSecond)
                .setNanos(now.nano)
                .build())
            .build()
    }
} 
