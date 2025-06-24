package io.sandonjacobs.streaming.parking.utils

import io.sandonjacobs.streaming.parking.model.*
import java.util.concurrent.atomic.AtomicLong

/**
 * Factory class for creating ParkingEvent instances with realistic data.
 */
object ParkingEventFactory {
    
    private val eventIdCounter = AtomicLong(0)
    
    /**
     * Creates a parking entry event for a specific parking space.
     */
    fun createEntryEvent(
        garageId: String,
        parkingSpace: ParkingSpace
    ): ParkingEvent {
        val vehicle = VehicleFactory.createVehicle(parkingSpace.type)
        
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
     * Creates a parking exit event for a vehicle.
     */
    fun createExitEvent(
        garageId: String,
        zoneId: String,
        spaceId: String,
        vehicle: Vehicle,
        location: Location? = null
    ): ParkingEvent {
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
     * Creates a random parking event (entry or exit).
     * Since we're not tracking active vehicles, we'll use a simple probability-based approach.
     */
    fun createRandomEvent(
        garageId: String,
        parkingSpace: ParkingSpace,
        location: Location? = null
    ): ParkingEvent {
        // Simple 70% chance of entry, 30% chance of exit
        val shouldCreateEntry = Math.random() < 0.7
        
        return if (shouldCreateEntry) {
            createEntryEvent(garageId, parkingSpace)
        } else {
            // For exit events, create a random vehicle
            val vehicle = VehicleFactory.createVehicle(parkingSpace.type)
            createExitEvent(garageId, parkingSpace.zoneId, parkingSpace.id, vehicle, location)
        }
    }
} 