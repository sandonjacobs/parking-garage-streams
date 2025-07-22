plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    alias(libs.plugins.kotlinPluginSerialization)
    // Create a fat JAR with all dependencies
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    // Apply the kotlinx bundle of dependencies from the version catalog (`gradle/libs.versions.toml`).
    implementation(libs.bundles.kotlinxEcosystem)

    // Command line argument parsing
    implementation(libs.kotlinxCLI)

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

    // Logging dependencies
    implementation(libs.slf4jApi)
    implementation(libs.logbackClassic)

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.kafkaStreamsTestUtils)
}

//// Configure the shadow JAR task
//tasks.shadowJar {
//    archiveBaseName.set("kafka-streams")
//    archiveClassifier.set("")
//    archiveVersion.set("")
//    manifest {
//        attributes(mapOf(
//            "Main-Class" to "io.sandonjacobs.parking.kstreams.MainClassKt"
//        ))
//    }
//}

// Make the build task depend on the shadowJar task
tasks.build {
    dependsOn(tasks.shadowJar)
}
