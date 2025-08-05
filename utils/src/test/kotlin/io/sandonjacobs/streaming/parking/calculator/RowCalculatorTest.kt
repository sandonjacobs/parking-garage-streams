package io.sandonjacobs.streaming.parking.calculator

import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.ParkingRow
import io.sandonjacobs.streaming.parking.model.ParkingSpace
import io.sandonjacobs.streaming.parking.model.ParkingZone
import io.sandonjacobs.streaming.parking.model.VehicleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class RowCalculatorTest {

    private lateinit var garage: ParkingGarage

    private val defaultCapacity: Int = 10
    private val handicapCapacity: Int = 2
    private val motorcycleCapacity: Int = 1

    @BeforeEach
    fun setup() {
        // Create a mock garage
        garage = createMockGarage("g002", 2, 1,
            defaultCapacity, handicapCapacity, motorcycleCapacity)
    }

    @Test
    fun `get capacity for a row with CAR type`() {
        val carSpace = findRandomParkingSpace(garage, VehicleType.CAR)
        val result = CapacityCalculator.calculateRowCapacity(garage, carSpace)
        assertEquals(defaultCapacity, result.carCapacity)
        assertEquals(handicapCapacity, result.handicapCapacity)
        assertEquals(motorcycleCapacity, result.motorcycleCapacity)
        assertEquals(CapacityType.ROW, result.type)
    }

    @Test
    fun `get capacity for a row with HANDICAP type`() {
        val handicapSpace = findRandomParkingSpace(garage, VehicleType.HANDICAP)
        val result = CapacityCalculator.calculateRowCapacity(garage, handicapSpace)
        assertEquals(defaultCapacity, result.carCapacity)
        assertEquals(handicapCapacity, result.handicapCapacity)
        assertEquals(motorcycleCapacity, result.motorcycleCapacity)
        assertEquals(CapacityType.ROW, result.type)
    }

    @Test
    fun `get capacity for a row with MOTORCYCLE type`() {
        val motorcycleSpace = findRandomParkingSpace(garage, VehicleType.MOTORCYCLE)
        val result = CapacityCalculator.calculateRowCapacity(garage, motorcycleSpace)
        assertEquals(defaultCapacity, result.carCapacity)
        assertEquals(handicapCapacity, result.handicapCapacity)
        assertEquals(motorcycleCapacity, result.motorcycleCapacity)
        assertEquals(CapacityType.ROW, result.type)
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