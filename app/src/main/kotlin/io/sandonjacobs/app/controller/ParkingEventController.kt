package io.sandonjacobs.app.controller

import io.sandonjacobs.app.service.ParkingEventGenerator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/parking-events")
class ParkingEventController(
    private val eventGenerator: ParkingEventGenerator
) {
    /**
     * Stops generating parking events for a specific garage.
     */
    @PostMapping("/stop/{garageId}")
    fun stopEventGenerationForGarage(
        @PathVariable garageId: String
    ): ResponseEntity<Map<String, Any>> {
        if (!eventGenerator.isGeneratingEvents(garageId)) {
            return ResponseEntity.badRequest().body(mapOf(
                "error" to "Event generation not running for garage: $garageId"
            ))
        }
        
        eventGenerator.stopGeneratingEvents(garageId)
        
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
        val activeGarages = eventGenerator.getActiveGarages()
        
        if (activeGarages.isEmpty()) {
            return ResponseEntity.ok(mapOf(
                "message" to "No active event generators found"
            ))
        }
        
        eventGenerator.stopAllGenerators()
        
        return ResponseEntity.ok(mapOf(
            "message" to "Stopped all event generators",
            "stoppedGarages" to activeGarages.toList()
        ))
    }

//    /**
//     * Gets the status of event generation for all garages.
//     */
//    @GetMapping("/status")
//    fun getEventGenerationStatus(): ResponseEntity<Map<String, Any>> {
//        val garages = parkingGarageConfigurationLoader.garages
//        val status = garages.map { garageConfig ->
//            val garageId = garageConfig.id
//            val isActive = eventGenerator.isGeneratingEvents(garageId)
//
//            mapOf(
//                "garageId" to garageId,
//                "name" to garageConfig.name,
//                "isGeneratingEvents" to isActive,
//                "totalSpaces" to calculateTotalSpaces(garageConfig)
//            )
//        }
//
//        val activeGarages = status.count { it["isGeneratingEvents"] as Boolean }
//
//        return ResponseEntity.ok(mapOf(
//            "totalGarages" to garages.size,
//            "activeGarages" to activeGarages,
//            "garages" to status,
//            "topic" to kafkaProducer.getTopicName()
//        ))
//    }

//    /**
//     * Gets the status of event generation for a specific garage.
//     */
//    @GetMapping("/status/{garageId}")
//    fun getEventGenerationStatusForGarage(
//        @PathVariable garageId: String
//    ): ResponseEntity<Map<String, Any>> {
//        val garages = parkingGarageConfigurationLoader.garages
//        val targetGarage = garages.find { it.id == garageId }
//
//        if (targetGarage == null) {
//            return ResponseEntity.badRequest().body(mapOf(
//                "error" to "Garage not found: $garageId"
//            ))
//        }
//
//        val isActive = eventGenerator.isGeneratingEvents(garageId)
//        val totalSpaces = calculateTotalSpaces(targetGarage)
//
//        return ResponseEntity.ok(mapOf(
//            "garageId" to garageId,
//            "name" to targetGarage.name,
//            "isGeneratingEvents" to isActive,
//            "totalSpaces" to totalSpaces
//        ))
//    }

//    /**
//     * Generates a single parking event for testing purposes.
//     */
//    @PostMapping("/send-single")
//    fun sendSingleEvent(
//        @RequestParam garageId: String,
//        @RequestParam spaceId: String,
//        @RequestParam(defaultValue = "ENTER") eventType: String
//    ): ResponseEntity<Map<String, Any>> {
//        val garage = garageService.getGarageById(garageId)
//            ?: return ResponseEntity.notFound().build()
//
//        val parkingSpace = garage.parkingZonesList
//            .flatMap { it.parkingRowsList }
//            .flatMap { it.parkingSpacesList }
//            .find { it.id == spaceId }
//            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Parking space not found"))
//
//        val event = ParkingEventFactory.createRandomEvent(garage.id, parkingSpace, garage.location)
//        kafkaProducer.sendParkingEvent(event)
//
//        return ResponseEntity.ok(mapOf(
//            "message" to "Single event sent",
//            "garageId" to garageId,
//            "spaceId" to spaceId,
//            "eventType" to event.type.name,
//            "topic" to kafkaProducer.getTopicName()
//        ))
//    }
//
//    /**
//     * Calculates the total number of parking spaces in a garage configuration.
//     */
//    private fun calculateTotalSpaces(garageConfig: io.sandonjacobs.app.config.ParkingGarageConfig): Int {
//        return garageConfig.parkingZones.sumOf { zone ->
//            zone.parkingRows.sumOf { row ->
//                row.parkingSpaces.size
//            }
//        }
//    }
} 