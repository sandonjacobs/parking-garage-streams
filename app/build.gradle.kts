plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    id("buildsrc.convention.kotlin-jvm")

    // Apply Spring Boot plugin
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
    implementation(project(":utils"))
    
    // Spring Boot dependencies
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // Spring Kafka for KafkaTemplate and producer functionality
    implementation("org.springframework.kafka:spring-kafka")
    
    // Configuration and YAML support
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Protobuf Java runtime (required for generated classes)
    implementation(libs.protobufJava)
    
    // Confluent Schema Registry and Protobuf serializer
    implementation("io.confluent:kafka-protobuf-serializer:7.9.1")
    implementation("io.confluent:kafka-schema-registry-client:7.9.1")
    
    // Coroutines support
    implementation(libs.kotlinxCoroutines)
    
    // Development tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.awaitility:awaitility:4.2.0")
}
