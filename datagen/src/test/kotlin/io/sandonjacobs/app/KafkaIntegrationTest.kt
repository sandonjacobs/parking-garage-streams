package io.sandonjacobs.app

import io.sandonjacobs.app.kafka.ParkingEventProducer
import io.sandonjacobs.streaming.parking.factory.ParkingEventFactory
import io.sandonjacobs.streaming.parking.factory.ParkingGarageFactory
import io.sandonjacobs.streaming.parking.model.Location
import io.sandonjacobs.streaming.parking.model.ParkingSpace
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.TimeUnit

@SpringBootTest(properties = ["spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}"])
@EmbeddedKafka(partitions = 1, topics = ["parking-events"])
@TestPropertySource(properties = [
    "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
    "spring.kafka.schema-registry.url=mock://test",
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.producer.value-serializer=io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer",
    "spring.kafka.producer.properties.schema.registry.url=mock://test",
    "spring.kafka.producer.properties.auto.register.schemas=true",
    "app.kafka.topic.parking-events=parking-events"
])
class KafkaIntegrationTest {

    @Autowired
    private lateinit var kafkaProducer: ParkingEventProducer

    @Test
    fun `should handle multiple parking events`() {
        // Given
        val topicName = "parking-events"
        val garageId = "test-garage"
        val zoneId = "zone-1"
        
        // Create a test garage with one zone containing one row with multiple parking spaces
        val parkingZone = ParkingGarageFactory.createParkingZone(zoneId, 0, 0, 3, garageId)
        val garage = ParkingGarageFactory.createParkingGarage(
            garageId, 
            arrayOf(parkingZone),
            Location.newBuilder().setLatitude("40.7128").setLongitude("-74.0060").build()
        )
        
        val parkingSpace: ParkingSpace = garage.parkingZonesList[0].parkingRowsList[0].parkingSpacesList[0]
        
        val events = (1..3).map { 
            ParkingEventFactory.createRandomEvent(garage.id, parkingSpace, garage.location)
        }

        // When
        val futures = events.map { event ->
            kafkaProducer.sendParkingEvent(event)
        }

        // Then
        futures.forEach { future ->
            val result = future.get(10, TimeUnit.SECONDS)
            assert(result.recordMetadata.topic() == topicName)
            assert(result.recordMetadata.partition() == 0)
        }
    }

    @Test
    fun `should send event synchronously`() {
        // Given
        val topicName = "parking-events"
        val garageId = "test-garage"
        val zoneId = "zone-1"
        
        // Create a test garage with one zone containing one row with one parking space
        val parkingZone = ParkingGarageFactory.createParkingZone(zoneId, 0, 0, 1, garageId)
        val garage = ParkingGarageFactory.createParkingGarage(
            garageId, 
            arrayOf(parkingZone),
            Location.newBuilder().setLatitude("40.7128").setLongitude("-74.0060").build()
        )
        
        val parkingSpace: ParkingSpace = garage.parkingZonesList[0].parkingRowsList[0].parkingSpacesList[0]
        val parkingEvent = ParkingEventFactory.createRandomEvent(garage.id, parkingSpace, garage.location)

        // When
        val result = kafkaProducer.sendParkingEventSync(parkingEvent)

        // Then
        assert(result.recordMetadata.topic() == topicName)
        assert(result.recordMetadata.partition() == 0)
        assert(result.recordMetadata.offset() >= 0)
    }
} 