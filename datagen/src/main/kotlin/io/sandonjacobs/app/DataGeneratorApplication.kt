package io.sandonjacobs.app

import io.sandonjacobs.app.config.ParkingGarageConfigurationLoader
import io.sandonjacobs.app.service.ParkingEventGenerator
import io.sandonjacobs.app.service.ParkingGarageService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
open class DataGeneratorApplication {

    @Bean
    open fun startupRunner(
        parkingGarageService: ParkingGarageService,
        configLoader: ParkingGarageConfigurationLoader,
        eventGenerator: ParkingEventGenerator
    ): CommandLineRunner {
        return CommandLineRunner { args ->
            println("🚗 Initializing Parking Garage Data Generator...")
            
            val garages = configLoader.garages.map { config ->
                parkingGarageService.createParkingGarage(config)
            }
            
            println("✅ Successfully initialized ${garages.size} parking garages:")
            
            garages.forEach { garage ->
                var totalSpaces = 0
                var totalRows = 0
                
                garage.parkingZonesList.forEach { zone ->
                    zone.parkingRowsList.forEach { row ->
                        totalSpaces += row.parkingSpacesList.size
                        totalRows++
                    }
                }
                
                println("   📍 ${garage.id}: ${garage.parkingZonesList.size} zones, $totalRows rows, $totalSpaces spaces")
                println("      Location: ${garage.location.latitude}, ${garage.location.longitude}")
            }
            
            val totalGarages = garages.size
            val totalZones = garages.sumOf { it.parkingZonesList.size }
            val totalRows = garages.sumOf { garage ->
                garage.parkingZonesList.sumOf { it.parkingRowsList.size }
            }
            val totalSpaces = garages.sumOf { garage ->
                garage.parkingZonesList.sumOf { zone ->
                    zone.parkingRowsList.sumOf { it.parkingSpacesList.size }
                }
            }
            
            println("\n📊 Summary:")
            println("   Total Garages: $totalGarages")
            println("   Total Zones: $totalZones")
            println("   Total Rows: $totalRows")
            println("   Total Spaces: $totalSpaces")
            
            // Start generating events for all garages
            println("\n🚀 Starting event generation and sending to Kafka...")
            garages.forEach { garage ->
                eventGenerator.startGeneratingEvents(garage, eventsPerMinute = 5)
                println("   📡 Started event generation for garage: ${garage.id}")
            }
            
            println("\n🎯 Application ready! Parking events are being generated and sent to Kafka topic '${eventGenerator.getTopicName()}' (${eventGenerator.getActiveGarages().size} garages active)")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<DataGeneratorApplication>(*args)
} 