package io.sandonjacobs.parking.kstreams.faker

import io.github.serpro69.kfaker.Faker
import io.sandonjacobs.streaming.parking.model.*

/**
 * A faker provider for generating random ParkingGarage instances for testing.
 */
class GarageFaker(private val faker: Faker = Faker()) {

    fun createMockGarage(
        garageId: String, howManyZones: Int = 1, rowsPerZone: Int = 1,
        defaultSpacesPerRow: Int = 20, handicapSpacesPerRow: Int = 4, motorcycleSpacesPerRow: Int = 1
    ): ParkingGarage {

        val garageBuilder = ParkingGarage.newBuilder()
        garageBuilder.setId(garageId)

        val zones = (1..howManyZones).map { zoneIndex ->
            val zoneId = "z${zoneIndex.toString().padStart(4, '0')}"
            val zoneBuilder = ParkingZone.newBuilder()
            zoneBuilder.setId(zoneId)

            val rows = (1..rowsPerZone).map { rowIndex ->
                val rowId = "r${rowIndex.toString().padStart(4, '0')}"

                val rowBuilder = ParkingRow.newBuilder()
                rowBuilder.setId(rowId)
                rowBuilder.addAllParkingSpaces(mkParkingSpaces(
                        VehicleType.DEFAULT,
                        "d",
                        defaultSpacesPerRow,
                        garageId,
                        zoneId,
                        rowId
                    )
                )
                rowBuilder.addAllParkingSpaces(
                    mkParkingSpaces(
                        VehicleType.HANDICAP,
                        "h",
                        handicapSpacesPerRow,
                        garageId,
                        zoneId,
                        rowId
                    )
                )
                rowBuilder.addAllParkingSpaces(
                    mkParkingSpaces(
                        VehicleType.MOTORCYCLE,
                        "m",
                        motorcycleSpacesPerRow,
                        garageId,
                        zoneId,
                        rowId
                    )
                )
                rowBuilder.build()
            }
            zoneBuilder.addAllParkingRows(rows)
            zoneBuilder.build()
        }

        garageBuilder.addAllParkingZones(zones)
        return garageBuilder.build()
    }

    private fun mkParkingSpaces(vehicleType: VehicleType, idPrefix: String, howMany: Int,
                                garageId: String, zoneId: String, rowId: String): List<ParkingSpace> {

        return (1..howMany).map { it ->
            val spaceId = "${idPrefix}${(it + 1).toString().padStart(4, '0')}"
            ParkingSpace.newBuilder()
                .setId(spaceId)
                .setType(vehicleType)
                .setGarageId(garageId)
                .setZoneId(zoneId)
                .setRowId(rowId)
                .build()
        }
    }
}