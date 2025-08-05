package io.sandonjacobs.streaming.parking.calculator

import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.ParkingRow
import io.sandonjacobs.streaming.parking.model.ParkingSpace
import io.sandonjacobs.streaming.parking.model.ParkingZone
import io.sandonjacobs.streaming.parking.model.VehicleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class ZoneCalculatorTest {

    private lateinit var garage: ParkingGarage

    private val defaultCapacity: Int = 10
    private val handicapCapacity: Int = 2
    private val motorcycleCapacity: Int = 1
    private val rowsPerZone: Int = 2

    @BeforeEach
    fun setup() {
        // Create a mock garage
        garage = createMockGarage("g002", 2, rowsPerZone,
            defaultCapacity, handicapCapacity, motorcycleCapacity)
    }

    @Test
    fun `get capacity for a zone with CAR type`() {
        val carSpace = findRandomParkingSpace(garage, VehicleType.CAR)
        val result = CapacityCalculator.calculateZoneCapacity(garage, carSpace)
        
        // Expected capacities are multiplied by the number of rows per zone
        val expectedCarCapacity = defaultCapacity * rowsPerZone
        val expectedHandicapCapacity = handicapCapacity * rowsPerZone
        val expectedMotorcycleCapacity = motorcycleCapacity * rowsPerZone
        
        assertEquals(expectedCarCapacity, result.carCapacity)
        assertEquals(expectedHandicapCapacity, result.handicapCapacity)
        assertEquals(expectedMotorcycleCapacity, result.motorcycleCapacity)
        assertEquals(CapacityType.ZONE, result.type)
    }

    @Test
    fun `get capacity for a zone with HANDICAP type`() {
        val handicapSpace = findRandomParkingSpace(garage, VehicleType.HANDICAP)
        val result = CapacityCalculator.calculateZoneCapacity(garage, handicapSpace)
        
        // Expected capacities are multiplied by the number of rows per zone
        val expectedCarCapacity = defaultCapacity * rowsPerZone
        val expectedHandicapCapacity = handicapCapacity * rowsPerZone
        val expectedMotorcycleCapacity = motorcycleCapacity * rowsPerZone
        
        assertEquals(expectedCarCapacity, result.carCapacity)
        assertEquals(expectedHandicapCapacity, result.handicapCapacity)
        assertEquals(expectedMotorcycleCapacity, result.motorcycleCapacity)
        assertEquals(CapacityType.ZONE, result.type)
    }

    @Test
    fun `get capacity for a zone with MOTORCYCLE type`() {
        val motorcycleSpace = findRandomParkingSpace(garage, VehicleType.MOTORCYCLE)
        val result = CapacityCalculator.calculateZoneCapacity(garage, motorcycleSpace)
        
        // Expected capacities are multiplied by the number of rows per zone
        val expectedCarCapacity = defaultCapacity * rowsPerZone
        val expectedHandicapCapacity = handicapCapacity * rowsPerZone
        val expectedMotorcycleCapacity = motorcycleCapacity * rowsPerZone
        
        assertEquals(expectedCarCapacity, result.carCapacity)
        assertEquals(expectedHandicapCapacity, result.handicapCapacity)
        assertEquals(expectedMotorcycleCapacity, result.motorcycleCapacity)
        assertEquals(CapacityType.ZONE, result.type)
    }

    // Helper methods

    private fun createMockGarage(
        garageId: String,
        zoneCount: Int,
        rowsPerZone: Int,
        carSpacesPerRow: Int,
        handicapSpacesPerRow: Int,
        motorcycleSpacesPerRow: Int
    ): ParkingGarage {
        val garageBuilder = ParkingGarage.newBuilder()
            .setId(garageId)

        for (zoneIndex in 1..zoneCount) {
            val zoneId = "zone-$zoneIndex"
            val zoneBuilder = ParkingZone.newBuilder()
                .setId(zoneId)

            for (rowIndex in 1..rowsPerZone) {
                val rowId = "row-$rowIndex"
                val rowBuilder = ParkingRow.newBuilder()
                    .setId(rowId)

                // Add car spaces
                for (spaceIndex in 1..carSpacesPerRow) {
                    val spaceId = "space-car-$zoneIndex-$rowIndex-$spaceIndex"
                    val spaceBuilder = ParkingSpace.newBuilder()
                        .setId(spaceId)
                        .setZoneId(zoneId)
                        .setRowId(rowId)
                        .setType(VehicleType.CAR)
                    rowBuilder.addParkingSpaces(spaceBuilder)
                }

                // Add handicap spaces
                for (spaceIndex in 1..handicapSpacesPerRow) {
                    val spaceId = "space-handicap-$zoneIndex-$rowIndex-$spaceIndex"
                    val spaceBuilder = ParkingSpace.newBuilder()
                        .setId(spaceId)
                        .setZoneId(zoneId)
                        .setRowId(rowId)
                        .setType(VehicleType.HANDICAP)
                    rowBuilder.addParkingSpaces(spaceBuilder)
                }

                // Add motorcycle spaces
                for (spaceIndex in 1..motorcycleSpacesPerRow) {
                    val spaceId = "space-motorcycle-$zoneIndex-$rowIndex-$spaceIndex"
                    val spaceBuilder = ParkingSpace.newBuilder()
                        .setId(spaceId)
                        .setZoneId(zoneId)
                        .setRowId(rowId)
                        .setType(VehicleType.MOTORCYCLE)
                    rowBuilder.addParkingSpaces(spaceBuilder)
                }

                zoneBuilder.addParkingRows(rowBuilder)
            }

            garageBuilder.addParkingZones(zoneBuilder)
        }

        return garageBuilder.build()
    }

    private fun findRandomParkingSpace(garage: ParkingGarage, vehicleType: VehicleType): ParkingSpace {
        val zones = garage.getParkingZonesList()
        val randomZone = zones.random()
        val rows = randomZone.getParkingRowsList()
        val randomRow = rows.random()
        val spaces = randomRow.getParkingSpacesList()
        return spaces.first { it.getType() == vehicleType }
    }
}