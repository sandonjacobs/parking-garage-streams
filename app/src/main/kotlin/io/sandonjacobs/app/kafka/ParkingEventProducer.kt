package io.sandonjacobs.app.kafka

import io.sandonjacobs.streaming.parking.model.ParkingEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
open class ParkingEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, ParkingEvent>,
    @Value("\${app.kafka.topic.parking-events:parking-events}")
    private val topicName: String
) {

    private val logger = LoggerFactory.getLogger(ParkingEventProducer::class.java)

    /**
     * Sends a ParkingEvent to Kafka with the parking space ID as the key.
     *
     * @param parkingEvent The ParkingEvent to send
     * @return CompletableFuture<SendResult> representing the send operation
     */
    open fun sendParkingEvent(parkingEvent: ParkingEvent): CompletableFuture<SendResult<String, ParkingEvent>> {
        val spaceId = parkingEvent.space.id
        val eventType = parkingEvent.type.name

        logger.debug("Sending $eventType event for space $spaceId to topic $topicName")

        val future = kafkaTemplate.send(topicName, spaceId, parkingEvent)

        return CompletableFuture.supplyAsync {
            try {
                future.get()
            } catch (e: Exception) {
                logger.error("Failed to send parking event for space $spaceId", e)
                throw e
            }
        }.whenComplete { result, throwable ->
            if (throwable == null) {
                logger.debug("Successfully sent parking event for space $spaceId to partition ${result.recordMetadata.partition()}, offset ${result.recordMetadata.offset()}")
            }
        }
    }

    /**
     * Sends a ParkingEvent synchronously and waits for the result.
     *
     * @param parkingEvent The ParkingEvent to send
     * @return SendResult containing metadata about the sent message
     */
    open fun sendParkingEventSync(parkingEvent: ParkingEvent): SendResult<String, ParkingEvent> {
        val spaceId = parkingEvent.space.id
        val eventType = parkingEvent.type.name

        logger.debug("Sending $eventType event for space $spaceId to topic $topicName (sync)")

        return try {
            kafkaTemplate.send(topicName, spaceId, parkingEvent).get()
        } catch (e: Exception) {
            logger.error("Failed to send parking event for space $spaceId", e)
            throw e
        }
    }

    /**
     * Gets the topic name being used for parking events.
     */
    open fun getTopicName(): String = topicName
}