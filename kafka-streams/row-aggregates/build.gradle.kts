plugins {
    // Create a fat JAR with all dependencies
    id("com.gradleup.shadow") version "9.1.0"
    application
}

dependencies {
    // Command line argument parsing
    implementation(libs.kotlinxCLI)
    implementation(project(":kafka-streams:kstreams-utils"))

    // All common dependencies are managed by the parent module
}

// Configure the shadow JAR task
tasks.shadowJar {
    archiveBaseName.set("row-aggregates")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes(mapOf(
            "Main-Class" to "io.sandonjacobs.parking.kstreams.MainClassKtKt"
        ))
    }
}

// Configure the application plugin
application {
    mainClass.set("io.sandonjacobs.parking.kstreams.MainClassKtKt")
}

// Make the build task depend on the shadowJar task
tasks.build {
    dependsOn(tasks.shadowJar)
}