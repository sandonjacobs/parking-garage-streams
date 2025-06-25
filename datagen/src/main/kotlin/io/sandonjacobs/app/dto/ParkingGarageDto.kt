package io.sandonjacobs.app.dto

import io.sandonjacobs.streaming.parking.model.*

/**
 * DTO for ParkingGarage to be serialized as JSON
 */
data class ParkingGarageDto(
    val id: String,
    val parkingZones: List<ParkingZoneDto>,
    val location: LocationDto?
)

/**
 * DTO for ParkingZone to be serialized as JSON
 */
data class ParkingZoneDto(
    val id: String,
    val parkingRows: List<ParkingRowDto>
)

/**
 * DTO for ParkingRow to be serialized as JSON
 */
data class ParkingRowDto(
    val id: String,
    val parkingSpaces: List<ParkingSpaceDto>
)

/**
 * DTO for ParkingSpace to be serialized as JSON
 */
data class ParkingSpaceDto(
    val id: String,
    val zoneId: String,
    val garageId: String,
    val type: String, // VehicleType as string
    val rowId: String?
)

/**
 * DTO for Location to be serialized as JSON
 */
data class LocationDto(
    val latitude: Double,
    val longitude: Double
)

/**
 * DTO for Vehicle to be serialized as JSON
 */
data class VehicleDto(
    val id: String,
    val licensePlate: String,
    val state: String,
    val type: String
)

/**
 * Extension functions to convert protobuf objects to DTOs
 */
fun ParkingGarage.toDto(): ParkingGarageDto {
    return ParkingGarageDto(
        id = id,
        parkingZones = parkingZonesList.map { it.toDto() },
        location = if (hasLocation()) location.toDto() else null
    )
}

fun ParkingZone.toDto(): ParkingZoneDto {
    return ParkingZoneDto(
        id = id,
        parkingRows = parkingRowsList.map { it.toDto() }
    )
}

fun ParkingRow.toDto(): ParkingRowDto {
    return ParkingRowDto(
        id = id,
        parkingSpaces = parkingSpacesList.map { it.toDto() }
    )
}

fun ParkingSpace.toDto(): ParkingSpaceDto {
    return ParkingSpaceDto(
        id = id,
        zoneId = zoneId,
        garageId = garageId,
        type = type.name,
        rowId = if (rowId.isNotEmpty()) rowId else null
    )
}

fun Location.toDto(): LocationDto {
    return LocationDto(
        latitude = latitude.toDouble(),
        longitude = longitude.toDouble()
    )
}

fun Vehicle.toDto(): VehicleDto {
    return VehicleDto(
        id = id,
        licensePlate = licensePlate,
        state = state,
        type = type.name
    )
} 