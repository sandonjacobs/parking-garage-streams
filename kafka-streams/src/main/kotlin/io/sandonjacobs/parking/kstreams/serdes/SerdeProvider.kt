package io.sandonjacobs.parking.kstreams.serdes

import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde
import io.sandonjacobs.streaming.parking.model.ParkingEvent
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import io.sandonjacobs.streaming.parking.status.SpaceStatus
import org.apache.kafka.common.serialization.Serde
import java.util.Properties

class SerdeProvider(private val kafkaProperties: Properties) {

    fun parkingEventSerde(isKey: Boolean): Serde<ParkingEvent> {
        val serde = KafkaProtobufSerde<ParkingEvent>()
        serde.configure(mkSerdeConfig() + mapOf(
            "specific.protobuf.value.type" to ParkingEvent::class.java.name
        ), isKey)
        return serde
    }

    fun parkingSpaceStatusSerde(isKey: Boolean): Serde<ParkingSpaceStatus> {
        val serde = KafkaProtobufSerde<ParkingSpaceStatus>()
        serde.configure(mkSerdeConfig() + mapOf(
            "specific.protobuf.value.type" to ParkingSpaceStatus::class.java.name,
        ), isKey)
        return serde
    }

    private fun mkSerdeConfig(): Map<String, Any?> {
        val config = when (kafkaProperties.containsKey("basic.auth.credentials.source")) {
            true -> {
                mapOf("schema.registry.url" to kafkaProperties.get("schema.registry.url"),
                    "basic.auth.credentials.source" to "USER_INFO",
                    "schema.registry.basic.auth.user.info" to kafkaProperties.get("basic.auth.user.info"))
            }
            false -> {
                mapOf("schema.registry.url" to kafkaProperties.get("schema.registry.url"))
            }
        }
        return config
    }

}