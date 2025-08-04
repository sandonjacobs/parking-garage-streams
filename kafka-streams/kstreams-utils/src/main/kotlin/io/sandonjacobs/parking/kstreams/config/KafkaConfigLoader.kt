package io.sandonjacobs.parking.kstreams.config

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import org.apache.kafka.streams.StreamsConfig
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Properties
import kotlin.collections.isNotEmpty


class KafkaConfigLoader {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        const val CC_BROKER = "CC_BROKER"
        const val CC_SCHEMA_REGISTRY_URL = "CC_SCHEMA_REGISTRY_URL"
        const val KAFKA_KEY_ID = "KAFKA_KEY_ID"
        const val KAFKA_KEY_SECRET = "KAFKA_KEY_SECRET"
        const val SCHEMA_REGISTRY_KEY_ID = "SCHEMA_REGISTRY_KEY_ID"
        const val SCHEMA_REGISTRY_KEY_SECRET = "SCHEMA_REGISTRY_KEY_SECRET"
    }

    /**
     * Loads properties from a file path. If the path starts with the system file separator,
     * it is treated as an absolute path. Otherwise, it is loaded from the classpath.
     *
     * @param filePath The path to the file (absolute if starts with system file separator, otherwise classpath)
     * @return Properties loaded from the file
     * @throws FileNotFoundException if the file cannot be found
     * @throws IllegalArgumentException if the file path is invalid
     */
    fun load(filePath: String?): Properties {

        if (filePath == null) {
            logger.warn("No file path provided. Returning empty properties object.")
            return Properties()
        }

        val properties = Properties()
        val inputStream: InputStream

        if (filePath.startsWith(File.separator)) {
            // Absolute path
            logger.info("Loading configuration from absolute file path: {}", filePath)
            inputStream = FileInputStream(filePath)
        } else {
            // Classpath resource
            logger.info("Loading configuration from classpath: {}", filePath)
            inputStream = javaClass.classLoader.getResourceAsStream(filePath)
                ?: throw FileNotFoundException("File not found in classpath: $filePath")
        }

        inputStream.use { stream ->
            properties.load(stream)
        }

        return properties
    }


    fun setupCloudStreamsConfig(applicationId: String, base: Properties, cloud: Properties? = null): Properties {
        if (cloud != null && cloud.isNotEmpty()) {
            logger.trace("Applying overrides from cloud configuration: {}", cloud)
            val properties = Properties()
            properties.putAll(base)

            // Add Kafka authentication properties
            properties.put("sasl.mechanism", "PLAIN")
            properties.put("security.protocol", "SASL_SSL")
            // Add other Confluent Cloud specific settings
            properties.put("client.dns.lookup", "use_all_dns_ips")

            // Add Confluent Cloud broker configuration
            cloud.getProperty(CC_BROKER)?.let {
                properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, it)
            }
            cloud.getProperty(KAFKA_KEY_ID)?.let { kafkaKey ->
                cloud.getProperty(KAFKA_KEY_SECRET)?.let { kafkaSecret ->
                    properties.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username='$kafkaKey' password='$kafkaSecret';")
                }
            }

            // Add Schema Registry URL
            cloud.getProperty(CC_SCHEMA_REGISTRY_URL)?.let {
                properties.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, it)
            }

            // Add Schema Registry authentication properties
            properties.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")

            cloud.getProperty(SCHEMA_REGISTRY_KEY_ID)?.let { schemaKey ->
                cloud.getProperty(SCHEMA_REGISTRY_KEY_SECRET)?.let { schemaSecret ->
                    properties.put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, "${schemaKey}:${schemaSecret}")
                }
            }

            properties.put("auto.register.schemas", "true")

            properties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId)
            return properties
        } else {
            logger.debug("Using base configuration: {}", base)

            val properties = Properties()
            properties.putAll(base)
            properties.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId)

            return properties
        }
    }

    fun mkSerdeConfig(schemaRegistryUrl: String, otherProps: Properties): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)

            otherProps.getProperty(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG)?.let { userInfo ->
                put(AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG, userInfo)
                put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")
            }
        }
    }
}