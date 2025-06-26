package io.sandonjacobs.parking.kstreams

import io.sandonjacobs.parking.kstreams.config.DefaultStreamsConfigLoader
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class KafkaStreamsApplication

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
        
//        // Load configuration based on provided option
//        val streamsProperties = when {
//            ccConfigFile != null && kafkaConfigFile != null -> {
//                throw IllegalArgumentException("Cannot specify both --cc-config and --kafka-config. Choose one.")
//            }
//            ccConfigFile != null -> {
//                logger.info("Loading Confluent Cloud configuration from: $ccConfigFile")
//                DefaultStreamsConfigLoader.loadConfluentCloudConfig(ccConfigFile!!)
//            }
//            kafkaConfigFile != null -> {
//                logger.info("Loading Kafka configuration from: $kafkaConfigFile")
//                DefaultStreamsConfigLoader.loadFromFile(kafkaConfigFile!!)
//            }
//            else -> {
//                throw IllegalArgumentException("Must specify either --cc-config or --kafka-config")
//            }
//        }

        logger.info("Built topology configuration")
        logger.info("Starting Spring Boot application...")

        // Start Spring Boot application for REST API
        SpringApplication.run(KafkaStreamsApplication::class.java, *args)
        
    } catch (e: Exception) {
        logger.error("Error: ${e.message}", e)
        System.err.println("Error: ${e.message}")
        System.err.println("Usage:")
        System.err.println("  --cc-config <file-path> or -c <file-path> (for Confluent Cloud)")
        System.err.println("  --kafka-config <file-path> or -k <file-path> (for generic Kafka)")
        System.exit(1)
    }
}