package io.sandonjacobs.streaming.parking.utils

import io.sandonjacobs.streaming.parking.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class ParkingEventFactoryTest {
    
    @BeforeEach
    fun setUp() {
        ParkingEventFactory.clearActiveVehicles()
    }
    
    @Test
    fun `should create entry event with correct data`() {
        // Given
        val garageId = "test-garage"
        val zoneId = "test-zone"
        val spaceId = "test-space"
        val parkingSpace = ParkingSpace.newBuilder()
            .setId(spaceId)
            .setZoneId(zoneId)
            .setGarageId(garageId)
            .setType(VehicleType.DEFAULT)
            .build()
        
        val location = Location.newBuilder()
            .setLatitude("37.7749")
            .setLongitude("-122.4194")
            .build()
        
        // When
        val event = ParkingEventFactory.createEntryEvent(garageId, parkingSpace)
        
        // Then
        assertEquals(ParkingEventType.ENTER, event.type)
        assertEquals(parkingSpace, event.space)
        assertEquals(VehicleType.DEFAULT, event.vehicle.type)
        assertNotNull(event.vehicle.licensePlate)
        assertNotNull(event.vehicle.state)
        assertTrue(event.timestamp.seconds > 0)
    }
    
    @Test
    fun `should create exit event with correct data`() {
        // Given
        val garageId = "test-garage"
        val zoneId = "test-zone"
        val spaceId = "test-space"
        val vehicle = Vehicle.newBuilder()
            .setId("test-vehicle")
            .setLicensePlate("ABC123")
            .setState("CA")
            .setType(VehicleType.DEFAULT)
            .build()
        
        val location = Location.newBuilder()
            .setLatitude("37.7749")
            .setLongitude("-122.4194")
            .build()
        
        // When
        val event = ParkingEventFactory.createExitEvent(garageId, zoneId, spaceId, vehicle, location)
        
        // Then
        assertEquals(ParkingEventType.EXIT, event.type)
        assertEquals(vehicle, event.vehicle)
        assertEquals(spaceId, event.space.id)
        assertEquals(zoneId, event.space.zoneId)
        assertEquals(garageId, event.space.garageId)
        assertTrue(event.timestamp.seconds > 0)
    }
    
    @Test
    fun `should track active vehicles correctly`() {
        // Given
        val garageId = "test-garage"
        val parkingSpace = ParkingSpace.newBuilder()
            .setId("test-space")
            .setZoneId("test-zone")
            .setGarageId(garageId)
            .setType(VehicleType.DEFAULT)
            .build()
        
        // When - create entry event
        val entryEvent = ParkingEventFactory.createEntryEvent(garageId, parkingSpace)
        
        // Then - vehicle should be tracked as active
        assertEquals(1, ParkingEventFactory.getActiveVehicleCount(garageId))
        
        // When - create exit event
        val exitEvent = ParkingEventFactory.createExitEvent(garageId, "test-zone", "test-space", entryEvent.vehicle)
        
        // Then - vehicle should no longer be tracked as active
        assertEquals(0, ParkingEventFactory.getActiveVehicleCount(garageId))
    }
    
    @Test
    fun `should create random events with realistic distribution`() {
        // Given
        val garageId = "test-garage"
        val parkingSpace = ParkingSpace.newBuilder()
            .setId("test-space")
            .setZoneId("test-zone")
            .setGarageId(garageId)
            .setType(VehicleType.DEFAULT)
            .build()
        
        // When - create multiple random events
        val events = (1..10).map {
            ParkingEventFactory.createRandomEvent(garageId, parkingSpace)
        }
        
        // Then - should have mix of entry and exit events
        val entryEvents = events.count { it.type == ParkingEventType.ENTER }
        val exitEvents = events.count { it.type == ParkingEventType.EXIT }
        
        assertTrue(entryEvents > 0, "Should have some entry events")
        assertTrue(exitEvents >= 0, "Should have zero or more exit events")
        assertEquals(10, entryEvents + exitEvents, "Total events should equal 10")
    }
    
//    @Test
//    fun `should generate unique event IDs`() {
//        // Given
//        val garageId = "test-garage"
//        val parkingSpace = ParkingSpace.newBuilder()
//            .setId("test-space")
//            .setZoneId("test-zone")
//            .setGarageId(garageId)
//            .setType(VehicleType.DEFAULT)
//            .build()
//
//        // When - create multiple events
//        val events = (1..5).map {
//            ParkingEventFactory.createEntryEvent(garageId, "test-zone", "test-space", parkingSpace)
//        }
//
//        // Then - all event IDs should be unique
//        val eventIds = events.map { it.id }.toSet()
//        assertEquals(5, eventIds.size, "All event IDs should be unique")
//    }
} 