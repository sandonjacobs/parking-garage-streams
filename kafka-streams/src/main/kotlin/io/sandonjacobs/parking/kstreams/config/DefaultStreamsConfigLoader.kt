package io.sandonjacobs.parking.kstreams.config

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde
import io.sandonjacobs.streaming.parking.model.ParkingEvent
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.KafkaStreamsConfiguration

@Configuration
@EnableKafkaStreams
@Profile("!cc")
open class DefaultStreamsConfigLoader(
    @Value(value = "\${spring.kafka.bootstrap-servers}") val bootstrapServers: String,
    @Value(value = "\${spring.kafka.properties.[schema.registry.url]}") val schemaRegistryUrl: String) : IStreamsConfigLoader {

    @Bean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    override fun streamsConfig(): KafkaStreamsConfiguration {
        return KafkaStreamsConfiguration(
            schemaRegistryProperties() + mapOf<String, Any>(
                StreamsConfig.APPLICATION_ID_CONFIG to "spring-cc-streams-app",
                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG to Serdes.String()::class.java,
                StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG to Serdes.String()::class.java
            )
        )
    }

    private fun schemaRegistryProperties(): Map<String, Any> {
        return mapOf(
            AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG to schemaRegistryUrl
        )
    }

    @Bean
    override fun parkingEventSerde(): Serde<ParkingEvent> {
        val serde = KafkaProtobufSerde<ParkingEvent>()
        serde.configure(schemaRegistryProperties() + mapOf("specific.protobuf.value.type" to ParkingEvent::class.java.name),
            false)
        return serde
    }

    @Bean
    override fun parkingSpaceStatusSerde(): Serde<ParkingSpaceStatus> {
        val serde = KafkaProtobufSerde<ParkingSpaceStatus>()
        serde.configure(schemaRegistryProperties() + mapOf("specific.protobuf.value.type" to ParkingSpaceStatus::class.java.name),
            false)
        return serde
    }

}