package io.sandonjacobs.streaming.parking.factory

import io.sandonjacobs.streaming.parking.model.*

/**
 * Factory class for creating ParkingGarage instances with predefined configurations.
 */
object ParkingGarageFactory {

    /**
     * Creates a new ParkingGarage with the provided array of ParkingZones.
     *
     * @param id The unique identifier for the parking garage
     * @param parkingZones Array of ParkingZone instances
     * @param location Optional location for the parking garage
     * @return A configured ParkingGarage instance
     */
    fun createParkingGarage(
        id: String,
        parkingZones: Array<ParkingZone>,
        location: Location? = null
    ): ParkingGarage {
        val builder = ParkingGarage.newBuilder()
            .setId(id)
            .addAllParkingZones(parkingZones.toList())
        
        location?.let { builder.setLocation(it) }
        
        return builder.build()
    }

    /**
     * Creates a ParkingZone with specified capacities for different vehicle types.
     *
     * @param zoneId The unique identifier for the parking zone
     * @param handicapCapacity Number of handicap parking spaces
     * @param motorcycleCapacity Number of motorcycle parking spaces
     * @param defaultCapacity Number of default parking spaces
     * @param garageId The ID of the parent garage
     * @return A configured ParkingZone instance
     * @throws IllegalArgumentException if any capacity is negative
     */
    fun createParkingZone(
        zoneId: String,
        handicapCapacity: Int,
        motorcycleCapacity: Int,
        defaultCapacity: Int,
        garageId: String
    ): ParkingZone {
        // Validate capacities
        require(handicapCapacity >= 0) { "Handicap capacity cannot be negative, got $handicapCapacity" }
        require(motorcycleCapacity >= 0) { "Motorcycle capacity cannot be negative, got $motorcycleCapacity" }
        require(defaultCapacity >= 0) { "Default capacity cannot be negative, got $defaultCapacity" }

        val parkingSpaces = createSpaces(zoneId, handicapCapacity, VehicleType.HANDICAP, "h", garageId) +
                           createSpaces(zoneId, motorcycleCapacity, VehicleType.MOTORCYCLE, "m", garageId) +
                           createSpaces(zoneId, defaultCapacity, VehicleType.CAR, "d", garageId)

        return ParkingZone.newBuilder()
            .setId(zoneId)
            .addAllParkingRows(
                listOf(
                    ParkingRow.newBuilder()
                        .setId("row-${zoneId}")
                        .addAllParkingSpaces(parkingSpaces)
                        .build()
                )
            )
            .build()
    }

    /**
     * Creates a ParkingZone with multiple rows, each containing the specified capacities.
     *
     * @param zoneId The unique identifier for the parking zone
     * @param numRows Number of parking rows in the zone
     * @param handicapCapacityPerRow Number of handicap parking spaces per row
     * @param motorcycleCapacityPerRow Number of motorcycle parking spaces per row
     * @param defaultCapacityPerRow Number of default parking spaces per row
     * @param garageId The ID of the parent garage
     * @return A configured ParkingZone instance with multiple rows
     */
    fun createParkingZoneWithRows(
        zoneId: String,
        numRows: Int,
        handicapCapacityPerRow: Int,
        motorcycleCapacityPerRow: Int,
        defaultCapacityPerRow: Int,
        garageId: String
    ): ParkingZone {
        // Validate inputs
        require(numRows > 0) { "Number of rows must be positive, got $numRows" }
        require(handicapCapacityPerRow >= 0) { "Handicap capacity per row cannot be negative, got $handicapCapacityPerRow" }
        require(motorcycleCapacityPerRow >= 0) { "Motorcycle capacity per row cannot be negative, got $motorcycleCapacityPerRow" }
        require(defaultCapacityPerRow >= 0) { "Default capacity per row cannot be negative, got $defaultCapacityPerRow" }

        val parkingRows = (0 until numRows).map { rowIndex ->
            val rowId = "row-${zoneId}-${rowIndex + 1}"
            val parkingSpaces = createSpaces(zoneId, handicapCapacityPerRow, VehicleType.HANDICAP, "h", garageId, rowId) +
                               createSpaces(zoneId, motorcycleCapacityPerRow, VehicleType.MOTORCYCLE, "m", garageId, rowId) +
                               createSpaces(zoneId, defaultCapacityPerRow, VehicleType.CAR, "d", garageId, rowId)

            ParkingRow.newBuilder()
                .setId(rowId)
                .addAllParkingSpaces(parkingSpaces)
                .build()
        }

        return ParkingZone.newBuilder()
            .setId(zoneId)
            .addAllParkingRows(parkingRows)
            .build()
    }

    /**
     * Helper function to create a list of parking spaces with the specified parameters.
     *
     * @param zoneId The zone identifier
     * @param count Number of spaces to create
     * @param type Vehicle type for the spaces
     * @param prefix Prefix for space ID (h=handicap, m=motorcycle, d=default)
     * @param garageId The garage identifier
     * @param rowId Optional row identifier (for multi-row zones)
     * @return List of ParkingSpace instances
     */
    private fun createSpaces(
        zoneId: String,
        count: Int,
        type: VehicleType,
        prefix: String,
        garageId: String,
        rowId: String? = null
    ): List<ParkingSpace> = List(count) { index ->
        ParkingSpace.newBuilder()
            .setId("space-${rowId ?: zoneId}-$prefix-${index + 1}")
            .setZoneId(zoneId)
            .setGarageId(garageId)
            .setType(type)
            .apply { rowId?.let { setRowId(it) } }
            .build()
    }
} 