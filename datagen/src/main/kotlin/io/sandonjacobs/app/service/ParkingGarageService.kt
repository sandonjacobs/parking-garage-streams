package io.sandonjacobs.app.service

import com.google.protobuf.Timestamp
import io.sandonjacobs.app.config.ParkingGarageConfig
import io.sandonjacobs.app.config.ParkingSpaceConfig
import io.sandonjacobs.app.config.ParkingZoneConfig
import io.sandonjacobs.app.kafka.ParkingEventProducer
import io.sandonjacobs.app.kafka.ParkingGarageProducer
import io.sandonjacobs.streaming.parking.model.*
import io.sandonjacobs.streaming.parking.factory.ParkingGarageFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ParkingGarageService(
    private val parkingGarageProducer: ParkingGarageProducer,
    private val parkingEventProducer: ParkingEventProducer) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun initGarageToKafka(parkingGarage: ParkingGarage) {
        logger.debug("Sending Parking garage: {} to kafka", parkingGarage.id)
        parkingGarageProducer.send(parkingGarage)
    }

    fun initEmptyGarageToKafka(garage: ParkingGarage) {
        for (z in garage.parkingZonesList) {
            for (r in z.parkingRowsList) {
                for (s in r.parkingSpacesList) {
                    val event = ParkingEvent.newBuilder()
                        .setSpace(s)
                        .setType(ParkingEventType.EXIT)
                        .setTimestamp(Timestamp.newBuilder().build())
                        .build()
                    parkingEventProducer.sendParkingEvent(event)
                }
            }
        }
    }

    /**
     * Converts a ParkingGarageConfig to a ParkingGarage protobuf object.
     */
    fun createParkingGarage(config: ParkingGarageConfig): ParkingGarage {
        val location = Location.newBuilder()
            .setLatitude(config.location.latitude)
            .setLongitude(config.location.longitude)
            .build()

        val parkingZones = config.zones.map { zoneConfig ->
            createParkingZone(zoneConfig, config.id)
        }.toTypedArray()

        return ParkingGarageFactory.createParkingGarage(
            id = config.id,
            parkingZones = parkingZones,
            location = location
        )
    }

    /**
     * Converts a ParkingZoneConfig to a ParkingZone protobuf object.
     */
    private fun createParkingZone(zoneConfig: ParkingZoneConfig, garageId: String): ParkingZone {
        return when {
            // Multi-row zone
            zoneConfig.rows != null -> {
                val parkingRows = zoneConfig.rows.map { rowConfig ->
                    val parkingSpaces = createParkingSpaces(
                        zoneConfig.id,
                        rowConfig.spaces,
                        garageId,
                        rowConfig.id
                    )
                    
                    ParkingRow.newBuilder()
                        .setId(rowConfig.id)
                        .addAllParkingSpaces(parkingSpaces)
                        .build()
                }

                ParkingZone.newBuilder()
                    .setId(zoneConfig.id)
                    .addAllParkingRows(parkingRows)
                    .build()
            }
            
            // Single-row zone - auto-generate rows
            zoneConfig.spaces != null -> {
                val totalSpaces = zoneConfig.spaces.handicap + zoneConfig.spaces.motorcycle + zoneConfig.spaces.default
                val spacesPerRow = 10 // You can adjust this to 12 if preferred
                val numberOfRows = (totalSpaces + spacesPerRow - 1) / spacesPerRow // Ceiling division
                
                val parkingRows = mutableListOf<ParkingRow>()
                var spaceIndex = 0
                
                for (rowNum in 1..numberOfRows) {
                    val rowId = "row-${zoneConfig.id}-${rowNum}"
                    val spacesInThisRow = mutableListOf<ParkingSpace>()
                    
                    // Calculate how many spaces should be in this row
                    val spacesForThisRow = if (rowNum == numberOfRows) {
                        totalSpaces - spaceIndex // Remaining spaces for last row
                    } else {
                        spacesPerRow
                    }
                    
                    // Distribute spaces across types for this row
                    val handicapForRow = minOf(zoneConfig.spaces.handicap - (parkingRows.sumOf { it.parkingSpacesList.count { space -> space.type == VehicleType.HANDICAP } }), spacesForThisRow)
                    val motorcycleForRow = minOf(zoneConfig.spaces.motorcycle - (parkingRows.sumOf { it.parkingSpacesList.count { space -> space.type == VehicleType.MOTORCYCLE } }), spacesForThisRow - handicapForRow)
                    val defaultForRow = spacesForThisRow - handicapForRow - motorcycleForRow
                    
                    // Create handicap spaces for this row
                    repeat(handicapForRow) { index ->
                        spacesInThisRow.add(
                            ParkingSpace.newBuilder()
                                .setId("space-${zoneConfig.id}-h-${spaceIndex + 1}")
                                .setZoneId(zoneConfig.id)
                                .setGarageId(garageId)
                                .setType(VehicleType.HANDICAP)
                                .setRowId(rowId)
                                .build()
                        )
                        spaceIndex++
                    }
                    
                    // Create motorcycle spaces for this row
                    repeat(motorcycleForRow) { index ->
                        spacesInThisRow.add(
                            ParkingSpace.newBuilder()
                                .setId("space-${zoneConfig.id}-m-${spaceIndex + 1}")
                                .setZoneId(zoneConfig.id)
                                .setGarageId(garageId)
                                .setType(VehicleType.MOTORCYCLE)
                                .setRowId(rowId)
                                .build()
                        )
                        spaceIndex++
                    }
                    
                    // Create default spaces for this row
                    repeat(defaultForRow) { index ->
                        spacesInThisRow.add(
                            ParkingSpace.newBuilder()
                                .setId("space-${zoneConfig.id}-d-${spaceIndex + 1}")
                                .setZoneId(zoneConfig.id)
                                .setGarageId(garageId)
                                .setType(VehicleType.CAR)
                                .setRowId(rowId)
                                .build()
                        )
                        spaceIndex++
                    }
                    
                    parkingRows.add(
                        ParkingRow.newBuilder()
                            .setId(rowId)
                            .addAllParkingSpaces(spacesInThisRow)
                            .build()
                    )
                }
                
                ParkingZone.newBuilder()
                    .setId(zoneConfig.id)
                    .addAllParkingRows(parkingRows)
                    .build()
            }
            
            else -> {
                throw IllegalArgumentException("Zone ${zoneConfig.id} must have either 'rows' or 'spaces' configuration")
            }
        }
    }

    /**
     * Creates a list of ParkingSpace objects from configuration.
     */
    private fun createParkingSpaces(
        zoneId: String,
        spaceConfig: ParkingSpaceConfig,
        garageId: String,
        rowId: String? = null
    ): List<ParkingSpace> {
        val spaces = mutableListOf<ParkingSpace>()
        
        // Create handicap spaces
        repeat(spaceConfig.handicap) { index ->
            spaces.add(
                ParkingSpace.newBuilder()
                    .setId("space-${rowId ?: zoneId}-h-${index + 1}")
                    .setZoneId(zoneId)
                    .setGarageId(garageId)
                    .setType(VehicleType.HANDICAP)
                    .apply { rowId?.let { setRowId(it) } }
                    .build()
            )
        }
        
        // Create motorcycle spaces
        repeat(spaceConfig.motorcycle) { index ->
            spaces.add(
                ParkingSpace.newBuilder()
                    .setId("space-${rowId ?: zoneId}-m-${index + 1}")
                    .setZoneId(zoneId)
                    .setGarageId(garageId)
                    .setType(VehicleType.MOTORCYCLE)
                    .apply { rowId?.let { setRowId(it) } }
                    .build()
            )
        }
        
        // Create default spaces
        repeat(spaceConfig.default) { index ->
            spaces.add(
                ParkingSpace.newBuilder()
                    .setId("space-${rowId ?: zoneId}-d-${index + 1}")
                    .setZoneId(zoneId)
                    .setGarageId(garageId)
                    .setType(VehicleType.CAR)
                    .apply { rowId?.let { setRowId(it) } }
                    .build()
            )
        }
        
        return spaces
    }
} 