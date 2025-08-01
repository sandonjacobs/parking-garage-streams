// This is a parent module that doesn't produce any artifacts itself
// It serves as a container for kafka-streams submodules and manages common dependencies

plugins {
    // Apply the shared build logic from a convention plugin.
    id("buildsrc.convention.kotlin-jvm") apply false
    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    alias(libs.plugins.kotlinPluginSerialization) apply false
}

// Define common dependencies for all Kafka Streams submodules
// These will be applied to all subprojects
val kafkaVersion = "3.9.1"
val confluentVersion = "7.9.1"
val slf4jVersion = "2.0.17"
val logbackVersion = "1.5.18"
val kFakerVersion = "2.0.0-rc.11"

// Configure all subprojects of the kafka-streams module
subprojects {
    // Apply common plugins to all submodules
    apply {
        plugin("buildsrc.convention.kotlin-jvm")
        plugin("org.jetbrains.kotlin.plugin.serialization")
    }

    // Common dependencies for all Kafka Streams submodules
    dependencies {
        // Project dependencies
        "implementation"(project(":utils"))
        
        // Kafka Streams dependencies
        "implementation"("org.apache.kafka:kafka-streams:$kafkaVersion")
        "implementation"("org.apache.kafka:kafka-clients:$kafkaVersion")
        
        // Protobuf dependencies
        "implementation"("com.google.protobuf:protobuf-java:3.25.3")
        "implementation"("io.confluent:kafka-protobuf-serializer:$confluentVersion")
        "implementation"("io.confluent:kafka-streams-protobuf-serde:$confluentVersion")
        "implementation"("io.confluent:kafka-schema-registry-client:$confluentVersion")
        
        // Logging dependencies
        "implementation"("org.slf4j:slf4j-api:$slf4jVersion")
        "implementation"("ch.qos.logback:logback-classic:$logbackVersion")
        
        // Test dependencies
        "testImplementation"(kotlin("test"))
        "testImplementation"("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")

        "testImplementation"("io.github.serpro69:kotlin-faker:${kFakerVersion}")

//        "testImplementation"("org.junit.jupiter:junit-jupiter-api:5.x.x")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:5.13.4")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:5.13.4")
    }
}

// Disable all tasks for this module as it's just a container
tasks.register("build") {
    // This is an empty task that does nothing
    // It's here just to satisfy the Gradle build lifecycle
}

tasks.register("clean") {
    // This is an empty task that does nothing
    // It's here just to satisfy the Gradle build lifecycle
}

tasks.register("check") {
    // This is an empty task that does nothing
    // It's here just to satisfy the Gradle build lifecycle
}