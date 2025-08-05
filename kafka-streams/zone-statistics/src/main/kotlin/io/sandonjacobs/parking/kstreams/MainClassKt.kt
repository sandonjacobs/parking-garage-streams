package io.sandonjacobs.parking.kstreams

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.sandonjacobs.parking.kstreams.config.KafkaConfigLoader
import io.sandonjacobs.parking.kstreams.serde.SerdeProvider
import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.status.ParkingGarageZoneStatus
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.slf4j.LoggerFactory


fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("MainClass")

    logger.info("Starting Zone Statistics Kafka Streams application...")

    // Parse command line arguments
    val parser = ArgParser("kstreams-zone-statistics-topology")

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

    val streamsConfig = kafkaConfigLoader.setupCloudStreamsConfig("kstreams-zone-statistics", kafkaConfig, cloudConfig)

    // Get schema registry URL from configuration
    val schemaRegistryUrl = streamsConfig.getProperty(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG)
    if (schemaRegistryUrl == null) {
        logger.error("Schema registry URL is required in the configuration file.")
        return
    }

    // Create serdes
    val serdeConfigMap = kafkaConfigLoader.mkSerdeConfig(schemaRegistryUrl, streamsConfig)
    val parkingSpaceStatusSerde = SerdeProvider.createProtobufSerde(ParkingSpaceStatus::class, serdeConfigMap, false)
    val garageSerde = SerdeProvider.createProtobufSerde(ParkingGarage::class, serdeConfigMap, false)
    val zoneAggregateSerde = SerdeProvider.createProtobufSerde(ParkingGarageZoneStatus::class, serdeConfigMap, false)

    // Create topology
    val builder = StreamsBuilder()
    val topology = ZoneStatisticsTopology(parkingSpaceStatusSerde, garageSerde, zoneAggregateSerde)
        .buildTopology(builder)

    // Create and start Kafka Streams
    val streams = KafkaStreams(topology, streamsConfig)

    // Add shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down Zone Statistics Kafka Streams application...")
        streams.close()
    })

    // Start streams
    streams.start()
    logger.info("Zone Statistics Kafka Streams application started")
}