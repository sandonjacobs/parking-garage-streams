package io.sandonjacobs.streaming.parking.factory

import io.sandonjacobs.streaming.parking.model.*
import kotlin.test.*

class ParkingGarageFactoryTest {

    @Test
    fun `createParkingGarage should create garage with provided zones`() {
        val zone1 = ParkingZone.newBuilder().setId("zone-1").build()
        val zone2 = ParkingZone.newBuilder().setId("zone-2").build()
        
        val garage = ParkingGarageFactory.createParkingGarage(
            id = "garage-1",
            parkingZones = arrayOf(zone1, zone2)
        )
        
        assertEquals("garage-1", garage.id)
        assertEquals(2, garage.parkingZonesCount)
        assertEquals("zone-1", garage.getParkingZones(0).id)
        assertEquals("zone-2", garage.getParkingZones(1).id)
    }

    @Test
    fun `createParkingGarage should create garage with location`() {
        val location = Location.newBuilder()
            .setLatitude("40.7128")
            .setLongitude("-74.0060")
            .build()
        
        val garage = ParkingGarageFactory.createParkingGarage(
            id = "garage-1",
            parkingZones = emptyArray(),
            location = location
        )
        
        assertTrue(garage.hasLocation())
        assertEquals("40.7128", garage.location.latitude)
        assertEquals("-74.0060", garage.location.longitude)
    }

    @Test
    fun `createParkingZone should create zone with correct space distribution`() {
        val zone = ParkingGarageFactory.createParkingZone(
            zoneId = "zone-1",
            handicapCapacity = 10,
            motorcycleCapacity = 5,
            defaultCapacity = 85,
            garageId = "garage-1"
        )
        
        assertEquals("zone-1", zone.id)
        assertEquals(1, zone.parkingRowsCount)
        
        val spaces = zone.getParkingRows(0).parkingSpacesList
        assertEquals(100, spaces.size) // 10 + 5 + 85 = 100
        
        val handicapSpaces = spaces.count { it.type == VehicleType.HANDICAP }
        val motorcycleSpaces = spaces.count { it.type == VehicleType.MOTORCYCLE }
        val defaultSpaces = spaces.count { it.type == VehicleType.CAR }
        
        assertEquals(10, handicapSpaces)
        assertEquals(5, motorcycleSpaces)
        assertEquals(85, defaultSpaces)
    }

    @Test
    fun `createParkingZone should validate capacities`() {
        assertFailsWith<IllegalArgumentException> {
            ParkingGarageFactory.createParkingZone(
                zoneId = "zone-1",
                handicapCapacity = -1, // Invalid: negative
                motorcycleCapacity = 5,
                defaultCapacity = 85,
                garageId = "garage-1"
            )
        }
        
        assertFailsWith<IllegalArgumentException> {
            ParkingGarageFactory.createParkingZone(
                zoneId = "zone-1",
                handicapCapacity = 10,
                motorcycleCapacity = -5, // Invalid: negative
                defaultCapacity = 85,
                garageId = "garage-1"
            )
        }
        
        assertFailsWith<IllegalArgumentException> {
            ParkingGarageFactory.createParkingZone(
                zoneId = "zone-1",
                handicapCapacity = 10,
                motorcycleCapacity = 5,
                defaultCapacity = -85, // Invalid: negative
                garageId = "garage-1"
            )
        }
    }

    @Test
    fun `createParkingZoneWithRows should create zone with multiple rows`() {
        val zone = ParkingGarageFactory.createParkingZoneWithRows(
            zoneId = "zone-1",
            numRows = 3,
            handicapCapacityPerRow = 2,
            motorcycleCapacityPerRow = 1,
            defaultCapacityPerRow = 17,
            garageId = "garage-1"
        )
        
        assertEquals("zone-1", zone.id)
        assertEquals(3, zone.parkingRowsCount)
        
        // Check each row
        for (rowIndex in 0 until 3) {
            val row = zone.getParkingRows(rowIndex)
            assertEquals("row-zone-1-${rowIndex + 1}", row.id)
            assertEquals(20, row.parkingSpacesCount) // 2 + 1 + 17 = 20
            
            val spaces = row.parkingSpacesList
            val handicapSpaces = spaces.count { it.type == VehicleType.HANDICAP }
            val motorcycleSpaces = spaces.count { it.type == VehicleType.MOTORCYCLE }
            val defaultSpaces = spaces.count { it.type == VehicleType.CAR }
            
            assertEquals(2, handicapSpaces)
            assertEquals(1, motorcycleSpaces)
            assertEquals(17, defaultSpaces)
        }
    }

    @Test
    fun `createParkingZoneWithRows should validate inputs`() {
        assertFailsWith<IllegalArgumentException> {
            ParkingGarageFactory.createParkingZoneWithRows(
                zoneId = "zone-1",
                numRows = 0, // Invalid: must be positive
                handicapCapacityPerRow = 2,
                motorcycleCapacityPerRow = 1,
                defaultCapacityPerRow = 17,
                garageId = "garage-1"
            )
        }
        
        assertFailsWith<IllegalArgumentException> {
            ParkingGarageFactory.createParkingZoneWithRows(
                zoneId = "zone-1",
                numRows = 3,
                handicapCapacityPerRow = -2, // Invalid: negative
                motorcycleCapacityPerRow = 1,
                defaultCapacityPerRow = 17,
                garageId = "garage-1"
            )
        }
    }

    @Test
    fun `space IDs should be properly formatted`() {
        val zone = ParkingGarageFactory.createParkingZone(
            zoneId = "zone-1",
            handicapCapacity = 2,
            motorcycleCapacity = 1,
            defaultCapacity = 7,
            garageId = "garage-1"
        )
        
        val spaces = zone.getParkingRows(0).parkingSpacesList
        
        // Check handicap space IDs
        assertTrue(spaces.any { it.id == "space-zone-1-h-1" && it.type == VehicleType.HANDICAP })
        assertTrue(spaces.any { it.id == "space-zone-1-h-2" && it.type == VehicleType.HANDICAP })
        
        // Check motorcycle space IDs
        assertTrue(spaces.any { it.id == "space-zone-1-m-1" && it.type == VehicleType.MOTORCYCLE })
        
        // Check default space IDs
        assertTrue(spaces.any { it.id == "space-zone-1-d-1" && it.type == VehicleType.CAR })
        assertTrue(spaces.any { it.id == "space-zone-1-d-7" && it.type == VehicleType.CAR })
    }

    @Test
    fun `should handle zero capacities`() {
        val zone = ParkingGarageFactory.createParkingZone(
            zoneId = "zone-1",
            handicapCapacity = 0,
            motorcycleCapacity = 0,
            defaultCapacity = 10,
            garageId = "garage-1"
        )
        
        val spaces = zone.getParkingRows(0).parkingSpacesList
        assertEquals(10, spaces.size)
        
        val handicapSpaces = spaces.count { it.type == VehicleType.HANDICAP }
        val motorcycleSpaces = spaces.count { it.type == VehicleType.MOTORCYCLE }
        val defaultSpaces = spaces.count { it.type == VehicleType.CAR }
        
        assertEquals(0, handicapSpaces)
        assertEquals(0, motorcycleSpaces)
        assertEquals(10, defaultSpaces)
    }
} 