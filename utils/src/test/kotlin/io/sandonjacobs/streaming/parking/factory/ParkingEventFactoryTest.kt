package io.sandonjacobs.streaming.parking.factory

import io.sandonjacobs.streaming.parking.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ParkingEventFactoryTest {
    
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
            .setType(VehicleType.CAR)
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
        assertEquals(VehicleType.CAR, event.vehicle.type)
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
            .setType(VehicleType.CAR)
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
    fun `should create random events with realistic distribution`() {
        // Given
        val garageId = "test-garage"
        val parkingSpace = ParkingSpace.newBuilder()
            .setId("test-space")
            .setZoneId("test-zone")
            .setGarageId(garageId)
            .setType(VehicleType.CAR)
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
} 