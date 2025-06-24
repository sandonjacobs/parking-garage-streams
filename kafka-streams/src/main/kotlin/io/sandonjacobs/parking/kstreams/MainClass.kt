package io.sandonjacobs.parking.kstreams

import io.sandonjacobs.parking.kstreams.config.StreamsConfigLoader
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType

fun main(args: Array<String>) {
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
        
        // Validate that exactly one config option is provided
        when {
            ccConfigFile != null && kafkaConfigFile != null -> {
                throw IllegalArgumentException("Cannot specify both --cc-config and --kafka-config. Choose one.")
            }
            ccConfigFile != null -> {
                val streamsProperties = StreamsConfigLoader.loadConfluentCloudConfig(ccConfigFile!!)
                println("Loaded Confluent Cloud configuration from: $ccConfigFile")
            }
            kafkaConfigFile != null -> {
                val streamsProperties = StreamsConfigLoader.loadFromFile(kafkaConfigFile!!)
                println("Loaded Kafka configuration from: $kafkaConfigFile")
            }
            else -> {
                throw IllegalArgumentException("Must specify either --cc-config or --kafka-config")
            }
        }
        
        // TODO: Initialize and start Kafka Streams application with the loaded properties
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        System.err.println("Usage:")
        System.err.println("  --cc-config <file-path> or -c <file-path> (for Confluent Cloud)")
        System.err.println("  --kafka-config <file-path> or -k <file-path> (for generic Kafka)")
        System.exit(1)
    }
}