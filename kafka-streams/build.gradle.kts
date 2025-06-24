plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    // Apply the kotlinx bundle of dependencies from the version catalog (`gradle/libs.versions.toml`).
    implementation(libs.bundles.kotlinxEcosystem)
    
    // Command line argument parsing
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    
    // Kafka Streams dependencies
    implementation(libs.kafkaStreams)
    implementation(libs.kafkaClients)
    
    // Protobuf dependencies
    implementation(libs.protobufJava)
    // Confluent Schema Registry and Protobuf serializer
    implementation(libs.confluentProtobuf)
    implementation(libs.confluentKafkaStreamsProtobuf)
    implementation(libs.confluentSchemaRegistryClient)

    // Project dependencies
    implementation(project(":utils"))

    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:2.0.17")
    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    implementation("ch.qos.logback:logback-classic:1.5.18")
    
    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.kafkaStreamsTestUtils)
}

