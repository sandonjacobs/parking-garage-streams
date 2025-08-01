package io.sandonjacobs.parking.kstreams

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.sandonjacobs.parking.kstreams.config.KafkaConfigLoader
import io.sandonjacobs.parking.kstreams.serde.SerdeProvider
import io.sandonjacobs.streaming.parking.model.ParkingEvent
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("MainClass")

    logger.info("Starting Kafka Streams application...")

    // Parse command line arguments
    val parser = ArgParser("kstreams-parking-space-status")

    val baseConfig by parser.option(ArgType.String,
        shortName = "b",
        fullName = "base",
        description = "Base Config for Kafka Streams application")
        .default("kafka-local.properties")

    val ccConfigOverride by parser.option(ArgType.String,
        shortName = "c",
        fullName = "cloud",
        description = "Confluent Cloud Config Path")

    parser.parse(args)

    val kafkaConfigLoader = KafkaConfigLoader()
    val kafkaConfig = kafkaConfigLoader.load(baseConfig)
    val cloudConfig = kafkaConfigLoader.load(ccConfigOverride)

    val streamsConfig = kafkaConfigLoader.setupCloudStreamsConfig(kafkaConfig, cloudConfig)

    // Get schema registry URL from configuration
    val schemaRegistryUrl = streamsConfig.getProperty(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG)
    if (schemaRegistryUrl == null) {
        logger.error("Schema registry URL is required in the configuration file.")
        return
    }

    // Create serdes
    val serdeConfigMap = kafkaConfigLoader.mkSerdeConfig(schemaRegistryUrl, streamsConfig)
    val parkingEventSerde = createParkingEventSerde(serdeConfigMap)
    val parkingSpaceStatusSerde = createParkingSpaceStatusSerde( serdeConfigMap)

    // Create topology
    val builder = StreamsBuilder()
    val topology = ParkingSpaceStatusTopology(parkingEventSerde, parkingSpaceStatusSerde)
        .buildTopology(builder)

    // Create and start Kafka Streams
    val streams = KafkaStreams(topology, streamsConfig)

    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down Kafka Streams application...")
        streams.close()
    })

    // Start streams
    streams.start()
    logger.info("Kafka Streams application started")
}

private fun createParkingEventSerde(otherProps: Map<String, String>): Serde<ParkingEvent> {
    return SerdeProvider.createProtobufSerde(ParkingEvent::class, otherProps, false)
}

private fun createParkingSpaceStatusSerde(otherProps: Map<String, String>): Serde<ParkingSpaceStatus> {
    return SerdeProvider.createProtobufSerde(ParkingSpaceStatus::class, otherProps, false)
}
