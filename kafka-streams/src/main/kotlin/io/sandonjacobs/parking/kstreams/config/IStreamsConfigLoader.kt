package io.sandonjacobs.parking.kstreams.config

import io.sandonjacobs.streaming.parking.model.ParkingEvent
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import org.apache.kafka.common.serialization.Serde
import org.springframework.kafka.config.KafkaStreamsConfiguration

interface IStreamsConfigLoader {

    fun streamsConfig(): KafkaStreamsConfiguration

    fun parkingEventSerde(): Serde<ParkingEvent>

    fun parkingSpaceStatusSerde(): Serde<ParkingSpaceStatus>

}