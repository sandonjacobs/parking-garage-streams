package io.sandonjacobs.app

import io.sandonjacobs.app.kafka.ParkingEventProducer
import io.sandonjacobs.streaming.parking.model.*
import io.sandonjacobs.streaming.parking.utils.ParkingEventFactory
import io.sandonjacobs.streaming.parking.utils.ParkingGarageFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.SendResult
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@SpringBootTest
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
    fun `should send parking event to kafka`() {
        // Given
        val topicName = "parking-events"
        val garageId = "test-garage"
        val zoneId = "zone-1"
        
        // Debug: Check when system property is set
        val brokerAddress = System.getProperty("spring.embedded.kafka.brokers", "localhost:9092")
        println("Embedded Kafka broker address: $brokerAddress")
        
        // Create a test garage with one zone containing one row with one parking space
        val parkingZone = ParkingGarageFactory.createParkingZone(zoneId, 0, 0, 1, garageId)
        val garage = ParkingGarageFactory.createParkingGarage(
            garageId, 
            arrayOf(parkingZone),
            Location.newBuilder().setLatitude("40.7128").setLongitude("-74.0060").build()
        )
        
        val parkingSpace: ParkingSpace = garage.parkingZonesList[0].parkingRowsList[0].parkingSpacesList[0]
        val parkingEvent = ParkingEventFactory.createRandomEvent(garage.id, parkingSpace, garage.location)
        val spaceId = parkingEvent.space.id

        // Create consumer to verify the message
        val consumerProps = mutableMapOf<String, Any>()
        consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokerAddress
        consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = "test-group"
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = "io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer"
        consumerProps["schema.registry.url"] = "mock://test"
        consumerProps["specific.protobuf.value.type"] = "io.sandonjacobs.streaming.parking.model.ParkingEvent"
        
        val consumerFactory = DefaultKafkaConsumerFactory<String, ParkingEvent>(consumerProps)
        val consumer = consumerFactory.createConsumer()
        consumer.subscribe(listOf(topicName))

        // When
        val future: CompletableFuture<SendResult<String, ParkingEvent>> = kafkaProducer.sendParkingEvent(parkingEvent)
        val result = future.get(10, TimeUnit.SECONDS)

        // Then
        assert(result.recordMetadata.topic() == topicName)
        assert(result.recordMetadata.partition() == 0)
        assert(result.recordMetadata.offset() >= 0)

        // Verify the message was received
        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted {
                val records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1))
                assert(records.count() > 0)
                
                val record: ConsumerRecord<String, ParkingEvent> = records.iterator().next()
                
                assert(record.key() == spaceId)
                assert(record.topic() == topicName)
                assert(record.partition() == 0)
                assert(record.value() != null)
                assert(record.value() is ParkingEvent)
            }

        consumer.close()
    }

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