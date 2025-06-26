package io.sandonjacobs.parking.kstreams

import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde
import io.sandonjacobs.streaming.parking.model.ParkingEvent
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import org.apache.kafka.common.serialization.Serde
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
open class TestConfig {

    @Bean
    @Primary
    open fun testParkingEventSerde(): Serde<ParkingEvent> {
        val serde = KafkaProtobufSerde<ParkingEvent>()
        val config = mapOf(
            "schema.registry.url" to "mock://test",
            "specific.protobuf.value.type" to "io.sandonjacobs.streaming.parking.model.ParkingEvent"
        )
        serde.configure(config, false)
        return serde
    }

    @Bean
    @Primary
    open fun testParkingSpaceStatusSerde(): Serde<ParkingSpaceStatus> {
        val serde = KafkaProtobufSerde<ParkingSpaceStatus>()
        val config = mapOf(
            "schema.registry.url" to "mock://test",
            "specific.protobuf.value.type" to "io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus"
        )
        serde.configure(config, false)
        return serde
    }
} 