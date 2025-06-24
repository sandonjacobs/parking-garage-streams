plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    alias(libs.plugins.kotlinPluginSerialization)
//    // Apply Protobuf plugin for Java code generation
//    alias(libs.plugins.protobuf)
}

dependencies {
    // Apply the kotlinx bundle of dependencies from the version catalog (`gradle/libs.versions.toml`).
    implementation(libs.bundles.kotlinxEcosystem)
    
    // Kafka Streams dependencies
    implementation(libs.kafkaStreams)
    implementation(libs.kafkaClients)
    
    // Protobuf dependencies
    implementation(libs.protobufJava)
    
    // Project dependencies
    implementation(project(":utils"))
    
    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation(libs.kafkaStreamsTestUtils)
}

//// Configure protobuf code generation
//protobuf {
//    protoc {
//        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
//    }
//}

//// Exclude .proto files from being processed as resources
//tasks.named<Copy>("processResources") {
//    exclude("**/*.proto")
//}
//
//// Configure source sets
//sourceSets {
//    main {
//        proto {
//            srcDir("src/main/proto")
//        }
//    }
//}