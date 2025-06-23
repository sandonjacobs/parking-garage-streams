package io.sandonjacobs.streaming.parking.utils

import io.sandonjacobs.streaming.parking.model.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Factory class for creating ParkingEvent instances with realistic data.
 */
object ParkingEventFactory {
    
    private val eventIdCounter = AtomicLong(0)
    private val activeVehicles = ConcurrentHashMap<String, MutableSet<String>>() // garageId -> Set<vehicleId>
    
    /**
     * Creates a parking entry event for a specific parking space.
     */
    fun createEntryEvent(
        garageId: String,
        parkingSpace: ParkingSpace
    ): ParkingEvent {
        val vehicle = VehicleFactory.createVehicle(parkingSpace.type)
        
        // Track this vehicle as active in the garage
        activeVehicles.computeIfAbsent(garageId) { mutableSetOf() }.add(vehicle.id)
        
        return ParkingEvent.newBuilder()
            .setType(ParkingEventType.ENTER)
            .setSpace(parkingSpace)
            .setVehicle(vehicle)
            .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(System.currentTimeMillis() / 1000)
                .setNanos(((System.currentTimeMillis() % 1000) * 1_000_000).toInt())
                .build())
            .build()
    }
    
    /**
     * Creates a parking exit event for a vehicle that was previously parked.
     */
    fun createExitEvent(
        garageId: String,
        zoneId: String,
        spaceId: String,
        vehicle: Vehicle,
        location: Location? = null
    ): ParkingEvent {
        // Remove this vehicle from active vehicles
        activeVehicles[garageId]?.remove(vehicle.id)
        
        // Create a parking space for the exit event
        val parkingSpace = ParkingSpace.newBuilder()
            .setId(spaceId)
            .setZoneId(zoneId)
            .setGarageId(garageId)
            .setType(vehicle.type)
            .build()
        
        return ParkingEvent.newBuilder()
            .setType(ParkingEventType.EXIT)
            .setSpace(parkingSpace)
            .setVehicle(vehicle)
            .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(System.currentTimeMillis() / 1000)
                .setNanos(((System.currentTimeMillis() % 1000) * 1_000_000).toInt())
                .build())
            .build()
    }
    
    /**
     * Creates a random parking event (entry or exit) based on current garage occupancy.
     */
    fun createRandomEvent(
        garageId: String,
        parkingSpace: ParkingSpace,
        location: Location? = null
    ): ParkingEvent {
        val activeVehiclesInGarage = activeVehicles[garageId]?.size ?: 0
        val totalSpaces = getTotalSpacesForGarage(garageId) // This would need to be passed or calculated
        
        // Determine if this should be an entry or exit based on occupancy
        val shouldCreateEntry = when {
            activeVehiclesInGarage == 0 -> true // Empty garage, definitely entry
            activeVehiclesInGarage >= totalSpaces -> false // Full garage, definitely exit
            else -> Math.random() < 0.6 // 60% chance of entry when partially occupied
        }
        
        return if (shouldCreateEntry) {
            createEntryEvent(garageId, parkingSpace)
        } else {
            // For exit, we need to find an existing vehicle
            val existingVehicleId = activeVehicles[garageId]?.random()
            if (existingVehicleId != null) {
                // Create a mock vehicle with the existing ID (in real scenario, you'd store the full vehicle)
                val vehicle = Vehicle.newBuilder()
                    .setId(existingVehicleId)
                    .setLicensePlate("EXIT-${existingVehicleId.takeLast(6)}")
                    .setState("CA")
                    .setType(parkingSpace.type)
                    .build()
                
                createExitEvent(garageId, parkingSpace.zoneId, parkingSpace.id, vehicle, location)
            } else {
                // Fallback to entry if no active vehicles found
                createEntryEvent(garageId, parkingSpace)
            }
        }
    }
    
    /**
     * Gets the total number of parking spaces for a garage.
     * This is a simplified version - in practice, you'd pass this information.
     */
    private fun getTotalSpacesForGarage(garageId: String): Int {
        // This would typically be calculated from the garage configuration
        // For now, return a reasonable default
        return 100
    }
    
    /**
     * Gets the current number of active vehicles in a garage.
     */
    fun getActiveVehicleCount(garageId: String): Int {
        return activeVehicles[garageId]?.size ?: 0
    }
    
    /**
     * Clears all active vehicles (useful for testing).
     */
    fun clearActiveVehicles() {
        activeVehicles.clear()
    }
} 