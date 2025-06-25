package io.sandonjacobs.app.controller

import io.sandonjacobs.app.config.ParkingGarageConfigurationLoader
import io.sandonjacobs.app.dto.ParkingGarageDto
import io.sandonjacobs.app.dto.toDto
import io.sandonjacobs.app.service.ParkingGarageService
import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.VehicleType
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/data-generator")
class DataGeneratorController(
    private val parkingGarageService: ParkingGarageService,
    private val configLoader: ParkingGarageConfigurationLoader
) {

    /**
     * Get all configured parking garages.
     */
    @GetMapping("/garages")
    fun getAllGarages(): List<ParkingGarageDto> {
        return configLoader.garages.map { config ->
            parkingGarageService.createParkingGarage(config).toDto()
        }
    }

    /**
     * Get a specific parking garage by ID.
     */
    @GetMapping("/garages/{garageId}")
    fun getGarageById(@PathVariable garageId: String): ParkingGarageDto? {
        return configLoader.garages
            .find { it.id == garageId }
            ?.let { parkingGarageService.createParkingGarage(it).toDto() }
    }

    /**
     * Get garage statistics.
     */
    @GetMapping("/garages/{garageId}/statistics")
    fun getGarageStatistics(@PathVariable garageId: String): Map<String, Any?>? {
        val garage = getGarageById(garageId) ?: return null
        
        var totalSpaces = 0
        var totalHandicapSpaces = 0
        var totalMotorcycleSpaces = 0
        var totalDefaultSpaces = 0
        
        for (zone in garage.parkingZones) {
            for (row in zone.parkingRows) {
                val spaces = row.parkingSpaces
                
                totalSpaces += spaces.size
                totalHandicapSpaces += spaces.count { it.type == "HANDICAP" }
                totalMotorcycleSpaces += spaces.count { it.type == "MOTORCYCLE" }
                totalDefaultSpaces += spaces.count { it.type == "DEFAULT" }
            }
        }
        
        return mapOf(
            "garageId" to garage.id,
            "totalSpaces" to totalSpaces,
            "handicapSpaces" to totalHandicapSpaces,
            "motorcycleSpaces" to totalMotorcycleSpaces,
            "defaultSpaces" to totalDefaultSpaces,
            "zones" to garage.parkingZones.size,
            "location" to garage.location?.let { mapOf(
                "latitude" to it.latitude,
                "longitude" to it.longitude
            ) }
        )
    }

    /**
     * Get all garage statistics.
     */
    @GetMapping("/statistics")
    fun getAllGarageStatistics(): Map<String, Any> {
        val garages = getAllGarages()
        val totalGarages = garages.size
        
        var totalSpaces = 0
        var totalHandicapSpaces = 0
        var totalMotorcycleSpaces = 0
        var totalDefaultSpaces = 0
        
        garages.forEach { garage ->
            for (zone in garage.parkingZones) {
                for (row in zone.parkingRows) {
                    val spaces = row.parkingSpaces
                    
                    totalSpaces += spaces.size
                    totalHandicapSpaces += spaces.count { it.type == "HANDICAP" }
                    totalMotorcycleSpaces += spaces.count { it.type == "MOTORCYCLE" }
                    totalDefaultSpaces += spaces.count { it.type == "DEFAULT" }
                }
            }
        }
        
        return mapOf(
            "totalGarages" to totalGarages,
            "totalSpaces" to totalSpaces,
            "handicapSpaces" to totalHandicapSpaces,
            "motorcycleSpaces" to totalMotorcycleSpaces,
            "defaultSpaces" to totalDefaultSpaces
        )
    }
} 