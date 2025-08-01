package io.sandonjacobs.app.kafka

import io.sandonjacobs.streaming.parking.model.ParkingGarage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service

@Service
open class ParkingGarageProducer(
    private val kafkaTemplate: KafkaTemplate<String, ParkingGarage>,
    @Value("\${app.kafka.topic.parking-garage:parking-garage}")
    private val garageTopicName: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    open fun send(garage: ParkingGarage): SendResult<String, ParkingGarage> {
        val garageId = garage.id

        logger.trace("Sending $garageId to topic $garageTopicName")
        return try {
            kafkaTemplate.send(garageTopicName, garageId, garage).get()
        } catch (e: Exception) {
            logger.error("Failed to send garage $garageId", e)
            throw e
        }
    }
}