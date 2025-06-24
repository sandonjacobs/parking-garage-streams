package io.sandonjacobs.app.controller

import io.sandonjacobs.app.service.ParkingEventGenerator
import io.sandonjacobs.app.service.ParkingGarageService
import io.sandonjacobs.app.config.ParkingGarageConfigurationLoader
import io.sandonjacobs.app.dto.ParkingEventDto
import io.sandonjacobs.streaming.parking.model.ParkingEvent
import io.sandonjacobs.streaming.parking.utils.ParkingEventFactory
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/api/parking-events")
class ParkingEventController(
    private val parkingEventGenerator: ParkingEventGenerator,
    private val parkingGarageService: ParkingGarageService,
    private val parkingGarageConfigurationLoader: ParkingGarageConfigurationLoader
) {
    
    private val logger = LoggerFactory.getLogger(ParkingEventController::class.java)
    private val eventCallbacks = ConcurrentHashMap<String, (ParkingEvent) -> Unit>()
    
    /**
     * Starts generating parking events for all configured garages.
     */
    @PostMapping("/start")
    fun startEventGeneration(
        @RequestParam(defaultValue = "10") eventsPerMinute: Int
    ): ResponseEntity<Map<String, Any>> {
        val garages = parkingGarageConfigurationLoader.garages
        val startedGarages = mutableListOf<String>()
        
        garages.forEach { garageConfig ->
            val garage = parkingGarageService.createParkingGarage(garageConfig)
            val garageId = garage.id
            
            if (!parkingEventGenerator.isGeneratingEvents(garageId)) {
                val callback: (ParkingEvent) -> Unit = { event ->
                    // Log the event (in a real application, you might send this to Kafka, etc.)
                    logger.info("Generated event: ${event.type} for garage ${event.space.garageId}, " +
                              "space ${event.space.id}, vehicle ${event.vehicle.licensePlate}")
                }
                
                eventCallbacks[garageId] = callback
                parkingEventGenerator.startGeneratingEvents(garage, eventsPerMinute, callback)
                startedGarages.add(garageId)
            }
        }
        
        return ResponseEntity.ok(mapOf(
            "message" to "Started event generation",
            "startedGarages" to startedGarages,
            "totalGarages" to garages.size,
            "eventsPerMinute" to eventsPerMinute
        ))
    }
    
    /**
     * Starts generating parking events for a specific garage.
     */
    @PostMapping("/start/{garageId}")
    fun startEventGenerationForGarage(
        @PathVariable garageId: String,
        @RequestParam(defaultValue = "10") eventsPerMinute: Int
    ): ResponseEntity<Map<String, Any>> {
        val garages = parkingGarageConfigurationLoader.garages
        val targetGarage = garages.find { it.id == garageId }
        
        if (targetGarage == null) {
            return ResponseEntity.badRequest().body(mapOf(
                "error" to "Garage not found: $garageId"
            ))
        }
        
        if (parkingEventGenerator.isGeneratingEvents(garageId)) {
            return ResponseEntity.badRequest().body(mapOf(
                "error" to "Event generation already running for garage: $garageId"
            ))
        }
        
        val garage = parkingGarageService.createParkingGarage(targetGarage)
        val callback: (ParkingEvent) -> Unit = { event ->
            logger.info("Generated event: ${event.type} for garage ${event.space.garageId}, " +
                      "space ${event.space.id}, vehicle ${event.vehicle.licensePlate}")
        }
        
        eventCallbacks[garageId] = callback
        parkingEventGenerator.startGeneratingEvents(garage, eventsPerMinute, callback)
        
        return ResponseEntity.ok(mapOf(
            "message" to "Started event generation for garage: $garageId",
            "garageId" to garageId,
            "eventsPerMinute" to eventsPerMinute
        ))
    }
    
    /**
     * Stops generating parking events for a specific garage.
     */
    @PostMapping("/stop/{garageId}")
    fun stopEventGenerationForGarage(
        @PathVariable garageId: String
    ): ResponseEntity<Map<String, Any>> {
        if (!parkingEventGenerator.isGeneratingEvents(garageId)) {
            return ResponseEntity.badRequest().body(mapOf(
                "error" to "Event generation not running for garage: $garageId"
            ))
        }
        
        parkingEventGenerator.stopGeneratingEvents(garageId)
        eventCallbacks.remove(garageId)
        
        return ResponseEntity.ok(mapOf(
            "message" to "Stopped event generation for garage: $garageId",
            "garageId" to garageId
        ))
    }
    
    /**
     * Stops generating parking events for all garages.
     */
    @PostMapping("/stop")
    fun stopAllEventGeneration(): ResponseEntity<Map<String, Any>> {
        val activeGarages = parkingEventGenerator.getActiveGarages()
        
        if (activeGarages.isEmpty()) {
            return ResponseEntity.ok(mapOf(
                "message" to "No active event generators found"
            ))
        }
        
        parkingEventGenerator.stopAllGenerators()
        eventCallbacks.clear()
        
        return ResponseEntity.ok(mapOf(
            "message" to "Stopped all event generators",
            "stoppedGarages" to activeGarages.toList()
        ))
    }
    
    /**
     * Gets the status of event generation for all garages.
     */
    @GetMapping("/status")
    fun getEventGenerationStatus(): ResponseEntity<Map<String, Any>> {
        val garages = parkingGarageConfigurationLoader.garages
        val status = garages.map { garageConfig ->
            val garageId = garageConfig.id
            val isActive = parkingEventGenerator.isGeneratingEvents(garageId)
            
            mapOf(
                "garageId" to garageId,
                "name" to garageConfig.name,
                "isGeneratingEvents" to isActive,
                "totalSpaces" to calculateTotalSpaces(garageConfig)
            )
        }
        
        val activeGarages = status.count { it["isGeneratingEvents"] as Boolean }
        
        return ResponseEntity.ok(mapOf(
            "totalGarages" to garages.size,
            "activeGarages" to activeGarages,
            "garages" to status
        ))
    }
    
    /**
     * Gets the status of event generation for a specific garage.
     */
    @GetMapping("/status/{garageId}")
    fun getEventGenerationStatusForGarage(
        @PathVariable garageId: String
    ): ResponseEntity<Map<String, Any>> {
        val garages = parkingGarageConfigurationLoader.garages
        val targetGarage = garages.find { it.id == garageId }
        
        if (targetGarage == null) {
            return ResponseEntity.badRequest().body(mapOf(
                "error" to "Garage not found: $garageId"
            ))
        }
        
        val isActive = parkingEventGenerator.isGeneratingEvents(garageId)
        val totalSpaces = calculateTotalSpaces(targetGarage)
        
        return ResponseEntity.ok(mapOf(
            "garageId" to garageId,
            "name" to targetGarage.name,
            "isGeneratingEvents" to isActive,
            "totalSpaces" to totalSpaces
        ))
    }
    
    /**
     * Generates a single parking event for testing purposes.
     */
    @PostMapping("/generate-single/{garageId}")
    fun generateSingleEvent(
        @PathVariable garageId: String
    ): ResponseEntity<Map<String, Any>> {
        val garages = parkingGarageConfigurationLoader.garages
        val targetGarage = garages.find { it.id == garageId }
        
        if (targetGarage == null) {
            return ResponseEntity.badRequest().body(mapOf(
                "error" to "Garage not found: $garageId"
            ))
        }
        
        val garage = parkingGarageService.createParkingGarage(targetGarage)
        val allSpaces = getAllParkingSpaces(garage)
        
        if (allSpaces.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf(
                "error" to "No parking spaces found for garage: $garageId"
            ))
        }
        
        val randomSpace = allSpaces.random()
        val event = ParkingEventFactory.createRandomEvent(garageId, randomSpace, garage.location)
        val eventDto = ParkingEventDto.fromParkingEvent(event)
        
        return ResponseEntity.ok(mapOf(
            "message" to "Generated single event",
            "event" to eventDto
        ))
    }
    
    /**
     * Calculates the total number of parking spaces in a garage configuration.
     */
    private fun calculateTotalSpaces(garageConfig: io.sandonjacobs.app.config.ParkingGarageConfig): Int {
        return garageConfig.zones.sumOf { zone ->
            when {
                zone.rows != null -> zone.rows.sumOf { row ->
                    row.spaces.handicap + row.spaces.motorcycle + row.spaces.default
                }
                zone.spaces != null -> {
                    zone.spaces.handicap + zone.spaces.motorcycle + zone.spaces.default
                }
                else -> 0
            }
        }
    }
    
    /**
     * Gets all parking spaces from a garage (flattened from zones and rows).
     */
    private fun getAllParkingSpaces(garage: io.sandonjacobs.streaming.parking.model.ParkingGarage): List<io.sandonjacobs.streaming.parking.model.ParkingSpace> {
        return garage.parkingZonesList.flatMap { zone ->
            zone.parkingRowsList.flatMap { row ->
                row.parkingSpacesList
            }
        }
    }
} 