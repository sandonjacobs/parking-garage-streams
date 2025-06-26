plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(libs.bundles.kotlinxEcosystem)

    implementation(libs.apacheFlinkTableAPI)
    implementation(libs.confluentFlinkTableAPI)

    // Add Flink dependencies here as needed
    testImplementation(kotlin("test"))
} 