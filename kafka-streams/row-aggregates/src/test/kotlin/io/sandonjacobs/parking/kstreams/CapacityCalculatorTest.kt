package io.sandonjacobs.parking.kstreams

import io.sandonjacobs.parking.kstreams.faker.GarageFaker
import io.sandonjacobs.streaming.parking.model.ParkingGarage
import io.sandonjacobs.streaming.parking.model.VehicleType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class CapacityCalculatorTest {

    private lateinit var garageFaker: GarageFaker

    private lateinit var garage: ParkingGarage

    private val defaultCapacity: Int = 10
    private val handicapCapacity: Int = 2
    private val motorcycleCapacity: Int = 1

    @BeforeEach
    fun setup() {
        // Initialize the faker
        garageFaker = GarageFaker()
        garage = garageFaker.createMockGarage("g002", 2, 1,
            defaultCapacity, handicapCapacity, motorcycleCapacity)
    }

    @ParameterizedTest
    @EnumSource(VehicleType::class, names = ["UNRECOGNIZED"], mode = EnumSource.Mode.EXCLUDE)
    fun `get capacity for a row, given a random parking space`(vehicleType: VehicleType) {
        val randomParkingSpace = garage.parkingZonesList.random().parkingRowsList.random().parkingSpacesList.first { it.type == vehicleType }
        val spaceVehicleType = randomParkingSpace.type

        val result = CapacityCalculator.findCapacityOfRow(garage, randomParkingSpace)

        when (spaceVehicleType) {
            VehicleType.DEFAULT -> assertEquals(defaultCapacity, result.defaultCapacity)
            VehicleType.HANDICAP -> assertEquals(handicapCapacity, result.handicapCapacity)
            VehicleType.MOTORCYCLE -> assertEquals(motorcycleCapacity, result.motorcycleCapacity)
            else -> throw IllegalStateException("Unknown vehicle type: $vehicleType")
        }
    }
}