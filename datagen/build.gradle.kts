plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    // Apply Spring Boot plugin from version catalog
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":utils"))
    
    // Spring Boot dependencies
    implementation(libs.springBootStarter)
    implementation(libs.springBootStarterWeb)
    implementation(libs.springBootStarterActuator)
    
    // Spring Kafka for KafkaTemplate and producer functionality
    implementation("org.springframework.kafka:spring-kafka")
    
    // Configuration and YAML support
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Protobuf Java runtime (required for generated classes)
    implementation(libs.protobufJava)
    
    // Confluent Schema Registry and Protobuf serializer
    implementation(libs.confluentProtobuf)
    implementation(libs.confluentSchemaRegistryClient)

    // Coroutines support
    implementation(libs.kotlinxCoroutines)
    
    // Development tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    // Test dependencies
    testImplementation(libs.springBootStarterTest)
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.awaitility:awaitility:4.2.0")
}
