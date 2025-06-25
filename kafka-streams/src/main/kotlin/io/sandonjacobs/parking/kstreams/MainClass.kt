package io.sandonjacobs.parking.kstreams

import io.sandonjacobs.parking.kstreams.config.StreamsConfigLoader
import io.sandonjacobs.parking.kstreams.serdes.SerdeProvider
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import org.apache.kafka.streams.KafkaStreams
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("MainClass")
    val parser = ArgParser("kafka-streams")
    val ccConfigFile by parser.option(
        ArgType.String,
        shortName = "c",
        fullName = "cc-config",
        description = "Path to a Confluent Cloud config file"
    )
    
    val kafkaConfigFile by parser.option(
        ArgType.String,
        shortName = "k",
        fullName = "kafka-config",
        description = "Path to a generic Kafka configuration file"
    )

    try {
        parser.parse(args)
        
        // Load configuration based on provided option
        val streamsProperties = when {
            ccConfigFile != null && kafkaConfigFile != null -> {
                throw IllegalArgumentException("Cannot specify both --cc-config and --kafka-config. Choose one.")
            }
            ccConfigFile != null -> {
                logger.info("Loading Confluent Cloud configuration from: $ccConfigFile")
                StreamsConfigLoader.loadConfluentCloudConfig(ccConfigFile!!)
            }
            kafkaConfigFile != null -> {
                logger.info("Loading Kafka configuration from: $kafkaConfigFile")
                StreamsConfigLoader.loadFromFile(kafkaConfigFile!!)
            }
            else -> {
                throw IllegalArgumentException("Must specify either --cc-config or --kafka-config")
            }
        }

        val serdeProvider = SerdeProvider(streamsProperties)
        
        // Build the topology
        val topology = ParkingSpaceStatusTopology(
            serdeProvider.parkingEventSerde(false),
            serdeProvider.parkingSpaceStatusSerde(false))
            .buildTopology()
        logger.info("Built topology: ${topology.describe()}")

        // Create and start the Kafka Streams application
        val streams = KafkaStreams(topology, streamsProperties)

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info("Shutting down Kafka Streams application...")
            streams.close()
        })

        // Start the streams application
        streams.start()
        logger.info("Kafka Streams application started successfully")

//        // Keep the application running
//        streams.close()
        
    } catch (e: Exception) {
        logger.error("Error: ${e.message}", e)
        System.err.println("Error: ${e.message}")
        System.err.println("Usage:")
        System.err.println("  --cc-config <file-path> or -c <file-path> (for Confluent Cloud)")
        System.err.println("  --kafka-config <file-path> or -k <file-path> (for generic Kafka)")
        System.exit(1)
    }
}