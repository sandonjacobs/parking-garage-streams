package io.sandonjacobs.app.config

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Configuration class for parking garage definitions loaded from YAML/JSON.
 */
data class ParkingGarageConfig(
    val id: String,
    val name: String,
    val location: LocationConfig,
    val zones: List<ParkingZoneConfig>
)

/**
 * Configuration for garage location.
 */
data class LocationConfig(
    val latitude: String,
    val longitude: String,
    val address: String? = null,
    val description: String? = null
)

/**
 * Configuration for parking zones within a garage.
 */
data class ParkingZoneConfig(
    val id: String,
    val name: String,
    val rows: List<ParkingRowConfig>? = null,
    val spaces: ParkingSpaceConfig? = null // For single-row zones
)

/**
 * Configuration for parking rows within a zone.
 */
data class ParkingRowConfig(
    val id: String,
    val name: String? = null,
    val spaces: ParkingSpaceConfig
)

/**
 * Configuration for parking spaces within a row or zone.
 */
data class ParkingSpaceConfig(
    val handicap: Int = 0,
    val motorcycle: Int = 0,
    val default: Int = 0
) 