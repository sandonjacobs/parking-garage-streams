package io.sandonjacobs.parking.kstreams.controller

import io.sandonjacobs.parking.kstreams.ParkingSpaceStatusQueryService
import io.sandonjacobs.streaming.parking.dto.ParkingSpaceStatusDto
import io.sandonjacobs.streaming.parking.dto.SpaceStatusDto
import io.sandonjacobs.streaming.parking.dto.toDto
import io.sandonjacobs.streaming.parking.dto.toDtoList
import io.sandonjacobs.streaming.parking.status.SpaceStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for querying parking space status from the materialized state store.
 * This controller runs within the same Kafka Streams application instance.
 */
@RestController
@RequestMapping("/api/parking-spaces")
class ParkingSpaceStatusController(
    private val queryService: ParkingSpaceStatusQueryService
) {

    /**
     * Get the status of a specific parking space by ID.
     *
     * @param spaceId The ID of the parking space
     * @return The parking space status or 404 if not found
     */
    @GetMapping("/{spaceId}")
    fun getParkingSpaceStatus(@PathVariable spaceId: String): ResponseEntity<ParkingSpaceStatusDto> {
        val status = queryService.getParkingSpaceStatus(spaceId)
        return if (status != null) {
            ResponseEntity.ok(status.toDto())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all parking space statuses.
     *
     * @return List of all parking space statuses
     */
    @GetMapping
    fun getAllParkingSpaceStatuses(): ResponseEntity<List<ParkingSpaceStatusDto>> {
        val statuses = queryService.getAllParkingSpaceStatuses()
        return ResponseEntity.ok(statuses.toDtoList())
    }

    /**
     * Get parking space statuses filtered by status.
     *
     * @param status The status to filter by (OCCUPIED or VACANT)
     * @return List of parking space statuses matching the filter
     */
    @GetMapping("/status/{status}")
    fun getParkingSpaceStatusesByStatus(@PathVariable status: String): ResponseEntity<List<ParkingSpaceStatusDto>> {
        val spaceStatus = try {
            SpaceStatus.valueOf(status.uppercase())
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        
        val statuses = queryService.getParkingSpaceStatusesByStatus(spaceStatus)
        return ResponseEntity.ok(statuses.toDtoList())
    }

    /**
     * Get counts of parking spaces by status.
     *
     * @return Map of status to count
     */
    @GetMapping("/counts")
    fun getParkingSpaceCounts(): ResponseEntity<Map<String, Int>> {
        val counts = queryService.getParkingSpaceCountsByStatus()
        val response = counts.mapKeys { it.key.name }
        return ResponseEntity.ok(response)
    }

    /**
     * Get the total count of parking spaces.
     *
     * @return Total count
     */
    @GetMapping("/count")
    fun getTotalParkingSpaceCount(): ResponseEntity<Map<String, Int>> {
        val count = queryService.getTotalParkingSpaceCount()
        return ResponseEntity.ok(mapOf("total" to count))
    }

    /**
     * Get parking spaces that are currently occupied.
     *
     * @return List of occupied parking spaces
     */
    @GetMapping("/occupied")
    fun getOccupiedParkingSpaces(): ResponseEntity<List<ParkingSpaceStatusDto>> {
        val occupied = queryService.getParkingSpaceStatusesByStatus(SpaceStatus.OCCUPIED)
        return ResponseEntity.ok(occupied.toDtoList())
    }

    /**
     * Get parking spaces that are currently vacant.
     *
     * @return List of vacant parking spaces
     */
    @GetMapping("/vacant")
    fun getVacantParkingSpaces(): ResponseEntity<List<ParkingSpaceStatusDto>> {
        val vacant = queryService.getParkingSpaceStatusesByStatus(SpaceStatus.VACANT)
        return ResponseEntity.ok(vacant.toDtoList())
    }

    /**
     * Check if the state store is ready for queries.
     *
     * @return Health status of the store
     */
    @GetMapping("/health")
    fun getHealthStatus(): ResponseEntity<Map<String, Any>> {
        val isReady = queryService.isStoreReady()
        val response = mapOf(
            "ready" to isReady,
            "status" to if (isReady) "HEALTHY" else "NOT_READY"
        )
        return ResponseEntity.ok(response)
    }
} 