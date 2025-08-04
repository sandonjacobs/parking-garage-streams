package io.sandonjacobs.parking.kstreams

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.sandonjacobs.parking.kstreams.config.KafkaConfigLoader
import io.sandonjacobs.parking.kstreams.serde.SerdeProvider
import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.status.ParkingGarageRowStatus
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.slf4j.LoggerFactory


fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("MainClass")

    logger.info("Starting Kafka Streams application...")

    // Parse command line arguments
    val parser = ArgParser("kstreams-row-aggregation-topology")

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

    val streamsConfig = kafkaConfigLoader.setupCloudStreamsConfig("kstreams-row-aggregations", kafkaConfig, cloudConfig)

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
    val rowAggregateSerde = SerdeProvider.createProtobufSerde(ParkingGarageRowStatus::class, serdeConfigMap, false)

    // Create topology
    val builder = StreamsBuilder()
    val topology = RowAggregationTopology(parkingSpaceStatusSerde, garageSerde, rowAggregateSerde)
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


