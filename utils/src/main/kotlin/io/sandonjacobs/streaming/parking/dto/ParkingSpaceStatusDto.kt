package io.sandonjacobs.streaming.parking.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * DTO for parking space status information.
 * This is the REST API representation of parking space status.
 */
data class ParkingSpaceStatusDto(
    val id: String,
    val space: ParkingSpaceDto,
    val status: SpaceStatusDto,
    val vehicle: VehicleDto? = null,
    @JsonProperty("last_updated")
    val lastUpdated: Instant
)

/**
 * DTO for parking space information.
 */
data class ParkingSpaceDto(
    val id: String,
    @JsonProperty("row_id")
    val rowId: String,
    @JsonProperty("zone_id")
    val zoneId: String,
    @JsonProperty("garage_id")
    val garageId: String,
    val type: VehicleTypeDto
)

/**
 * DTO for vehicle information.
 */
data class VehicleDto(
    val id: String,
    @JsonProperty("license_plate")
    val licensePlate: String,
    val state: String,
    val type: VehicleTypeDto
)

/**
 * DTO for space status enum.
 */
enum class SpaceStatusDto {
    VACANT,
    OCCUPIED
}

/**
 * DTO for vehicle type enum.
 */
enum class VehicleTypeDto {
    DEFAULT,
    HANDICAP,
    MOTORCYCLE
} 