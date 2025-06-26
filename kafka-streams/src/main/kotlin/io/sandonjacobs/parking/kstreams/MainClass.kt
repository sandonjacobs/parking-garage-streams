package io.sandonjacobs.parking.kstreams

import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class KafkaStreamsApplication

fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("MainClass")

   logger.info("Starting Spring Boot application...")

    // Start Spring Boot application for REST API
    SpringApplication.run(KafkaStreamsApplication::class.java, *args)
        
}