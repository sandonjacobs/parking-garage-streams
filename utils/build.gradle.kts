plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")
    // Apply Kotlin Serialization plugin from `gradle/libs.versions.toml`.
    alias(libs.plugins.kotlinPluginSerialization)
    // Apply Protobuf plugin for Java code generation
    alias(libs.plugins.protobuf)
}

dependencies {
    // Apply the kotlinx bundle of dependencies from the version catalog (`gradle/libs.versions.toml`).
    implementation(libs.bundles.kotlinxEcosystem)

    // Command line argument parsing
    implementation(libs.kotlinxCLI)

    // Add protobuf Java runtime
    implementation(libs.protobufJava)
    // Add Jackson for JSON serialization support in DTOs
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    testImplementation(kotlin("test"))
}

// Configure protobuf code generation
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
}

sourceSets {
    main {
        proto {
            srcDir("src/main/proto")
        }
    }
}

tasks.named<Copy>("processResources") {
    exclude("**/*.proto")
}

// Handle duplicate protobuf generated classes
tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}