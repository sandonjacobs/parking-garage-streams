package io.sandonjacobs.parking.kstreams.config

import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde
import org.apache.kafka.common.serialization.Serdes
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.Properties

object StreamsConfigLoader {

    private val logger = LoggerFactory.getLogger(StreamsConfigLoader::class.java)

    fun loadConfluentCloudConfig(filePath: String): Properties {

        logger.debug("Loading configuration from {}", filePath)

        val file = File(filePath)
        val ccProperties = Properties()
        ccProperties.load(FileInputStream(file))

        logger.trace("Loaded Confluent Cloud Properties -> {}", ccProperties)

        val properties = Properties().apply {
            put("bootstrap.servers", ccProperties.get("CC_BROKER"))
            put("security.protocol", "SASL_SSL")
            put("sasl.mechanism", "PLAIN")
            put("sasl.jaas.config", """
                org.apache.kafka.common.security.plain.PlainLoginModule required
                username='${ccProperties.get("KAFKA_KEY_ID")}'
                password='${ccProperties.get("KAFKA_KEY_SECRET")}';
            """.trimIndent())
            put("application.id", "parking-garage-kafka-streams")
            put("auto.offset.reset", "earliest")
            put("acks", "all")
            put("retries", "10")

            put("default.key.serde", Serdes.String()::class.java)
            put("default.value.serde", KafkaProtobufSerde::class.java)

            put("schema.registry.url", ccProperties.get("CC_SCHEMA_REGISTRY_URL"))
            put("basic.auth.credentials.source", "USER_INFO")
            put("basic.auth.user.info", "${ccProperties.get("SCHEMA_REGISTRY_KEY_ID")}:${ccProperties.get("SCHEMA_REGISTRY_KEY_SECRET")}")

            put("commit.interval.ms", "5000")
            put("cache.max.bytes.buffering", "10485760")
       }
       return properties
    }

    fun loadFromFile(kafkaConfigFile: String): Properties {
        logger.info("Loading configuration from {}", kafkaConfigFile)
        val properties = Properties()
        properties.load(FileInputStream(kafkaConfigFile))

        logger.trace("Loaded configuration from {}", properties)
        return properties
    }
}