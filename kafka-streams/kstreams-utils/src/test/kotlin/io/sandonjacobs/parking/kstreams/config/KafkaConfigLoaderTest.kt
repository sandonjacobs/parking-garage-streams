package io.sandonjacobs.parking.kstreams.config

import org.apache.kafka.streams.StreamsConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import java.util.Properties
import kotlin.test.assertTrue

class KafkaConfigLoaderTest {

    @TempDir
    lateinit var tempDir: Path


    val kafkaConfigLoader = KafkaConfigLoader()

    @Test
    fun `load should load properties from classpath resource`() {
        // When
        val properties = kafkaConfigLoader.load("test-config.properties")

        // Then
        assertNotNull(properties)
        assertEquals("value1", properties.getProperty("test.property.one"))
        assertEquals("value2", properties.getProperty("test.property.two"))
        assertEquals("value3", properties.getProperty("test.property.three"))
        assertEquals("test-app", properties.getProperty("application.id"))
        assertEquals("localhost:9092", properties.getProperty("bootstrap.servers"))
        assertEquals("http://localhost:8081", properties.getProperty("schema.registry.url"))
    }

    @Test
    fun `load should load properties from absolute path`() {
        // Given
        val tempFile = File(tempDir.toFile(), "absolute-path-config.properties")
        tempFile.writeText("""
            test.absolute.property.one=absolute1
            test.absolute.property.two=absolute2
            application.id=absolute-app
            bootstrap.servers=absolute:9092
            schema.registry.url=http://absolute:8081
        """.trimIndent())

        // When
        val properties = kafkaConfigLoader.load(tempFile.absolutePath)

        // Then
        assertNotNull(properties)
        assertEquals("absolute1", properties.getProperty("test.absolute.property.one"))
        assertEquals("absolute2", properties.getProperty("test.absolute.property.two"))
        assertEquals("absolute-app", properties.getProperty("application.id"))
        assertEquals("absolute:9092", properties.getProperty("bootstrap.servers"))
        assertEquals("http://absolute:8081", properties.getProperty("schema.registry.url"))
    }

    @Test
    fun `load should throw FileNotFoundException for non-existent classpath resource`() {
        // When/Then
        val exception = assertThrows<FileNotFoundException> {
            kafkaConfigLoader.load("non-existent-file.properties")
        }
        
        assertTrue(exception.message!!.contains("File not found in classpath"))
    }

    @Test
    fun `load should return an empty Properties object for a null path`() {

        val properties = kafkaConfigLoader.load(null)
        assertTrue(properties.isEmpty())
    }

    @Test
    fun `load should throw FileNotFoundException for non-existent absolute path`() {
        // Given
        val nonExistentFile = File(tempDir.toFile(), "non-existent-file.properties")
        
        // When/Then
        assertThrows<FileNotFoundException> {
            kafkaConfigLoader.load(nonExistentFile.absolutePath)
        }
    }
    
    @Test
    fun `setupCloudStreamsConfig should apply cloud properties to base properties`() {
        // Given
        val baseProperties = Properties().apply {
            setProperty("bootstrap.servers", "localhost:9092")
            setProperty("schema.registry.url", "http://localhost:8081")
        }
        
        val cloudProperties = Properties().apply {
            setProperty(KafkaConfigLoader.CC_BROKER, "pkc-abcde.us-west-2.aws.confluent.cloud:9092")
            setProperty(KafkaConfigLoader.CC_SCHEMA_REGISTRY_URL, "https://psrc-abcde.us-west-2.aws.confluent.cloud")
            setProperty(KafkaConfigLoader.KAFKA_KEY_ID, "KAFKA_KEY_ID")
            setProperty(KafkaConfigLoader.KAFKA_KEY_SECRET, "KAFKA_KEY_SECRET")
            setProperty(KafkaConfigLoader.SCHEMA_REGISTRY_KEY_ID, "SCHEMA_REGISTRY_KEY_ID")
            setProperty(KafkaConfigLoader.SCHEMA_REGISTRY_KEY_SECRET, "SCHEMA_REGISTRY_KEY_SECRET")
        }

        // When
        val result = kafkaConfigLoader.setupCloudStreamsConfig("test-app", baseProperties, cloudProperties)

        val expected = Properties().apply {
            putAll(baseProperties)
            putAll(cloudProperties)
            put(StreamsConfig.APPLICATION_ID_CONFIG, "test-app")
        }

        // Then
        assertEquals("test-app", result.getProperty("application.id"))
        assertEquals("pkc-abcde.us-west-2.aws.confluent.cloud:9092", result.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG))
        assertEquals("https://psrc-abcde.us-west-2.aws.confluent.cloud", result.getProperty("schema.registry.url"))
        assertEquals("PLAIN", result.getProperty("sasl.mechanism"))
        assertEquals("org.apache.kafka.common.security.plain.PlainLoginModule required username='KAFKA_KEY_ID' password='KAFKA_KEY_SECRET';", 
            result.getProperty("sasl.jaas.config"))
        assertEquals("SASL_SSL", result.getProperty("security.protocol"))
        assertEquals("USER_INFO", result.getProperty("basic.auth.credentials.source"))
        assertEquals("SCHEMA_REGISTRY_KEY_ID:SCHEMA_REGISTRY_KEY_SECRET", result.getProperty("basic.auth.user.info"))
        assertEquals("use_all_dns_ips", result.getProperty("client.dns.lookup"))
    }
    
    @Test
    fun `setupCloudStreamsConfig should return base properties when cloud properties are null`() {
        // Given
        val baseProperties = Properties().apply {
            setProperty("bootstrap.servers", "localhost:9092")
            setProperty("schema.registry.url", "http://localhost:8081")
        }
        
        // When
        val result = kafkaConfigLoader.setupCloudStreamsConfig("test-app", baseProperties, null)

        val expected = Properties().apply {
            putAll(baseProperties)
            put(StreamsConfig.APPLICATION_ID_CONFIG, "test-app")
        }
        // Then
        assertEquals(expected, result)
    }
}