package io.sandonjacobs.streaming.parking.dto

import io.sandonjacobs.streaming.parking.model.ParkingSpace
import io.sandonjacobs.streaming.parking.model.Vehicle
import io.sandonjacobs.streaming.parking.model.VehicleType
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import io.sandonjacobs.streaming.parking.status.SpaceStatus
import java.time.Instant

/**
 * Extension function to convert ParkingSpaceStatus protobuf to DTO.
 */
fun ParkingSpaceStatus.toDto(): ParkingSpaceStatusDto {
    return ParkingSpaceStatusDto(
        id = this.id,
        space = this.space.toDto(),
        status = this.status.toDto(),
        vehicle = this.vehicle?.toDto(),
        lastUpdated = Instant.ofEpochSecond(
            this.lastUpdated.seconds,
            this.lastUpdated.nanos.toLong()
        )
    )
}

/**
 * Extension function to convert ParkingSpace protobuf to DTO.
 */
fun ParkingSpace.toDto(): ParkingSpaceDto {
    return ParkingSpaceDto(
        id = this.id,
        rowId = this.rowId,
        zoneId = this.zoneId,
        garageId = this.garageId,
        type = this.type.toDto()
    )
}

/**
 * Extension function to convert Vehicle protobuf to DTO.
 */
fun Vehicle.toDto(): VehicleDto {
    return VehicleDto(
        id = this.id,
        licensePlate = this.licensePlate,
        state = this.state,
        type = this.type.toDto()
    )
}

/**
 * Extension function to convert SpaceStatus protobuf enum to DTO enum.
 */
fun SpaceStatus.toDto(): SpaceStatusDto {
    return when (this) {
        SpaceStatus.VACANT -> SpaceStatusDto.VACANT
        SpaceStatus.OCCUPIED -> SpaceStatusDto.OCCUPIED
        else -> throw IllegalArgumentException("Unknown SpaceStatus: $this")
    }
}

/**
 * Extension function to convert VehicleType protobuf enum to DTO enum.
 */
fun VehicleType.toDto(): VehicleTypeDto {
    return when (this) {
        VehicleType.CAR -> VehicleTypeDto.DEFAULT
        VehicleType.HANDICAP -> VehicleTypeDto.HANDICAP
        VehicleType.MOTORCYCLE -> VehicleTypeDto.MOTORCYCLE
        else -> throw IllegalArgumentException("Unknown VehicleType: $this")
    }
}

/**
 * Extension function to convert a list of ParkingSpaceStatus protobufs to DTOs.
 */
fun List<ParkingSpaceStatus>.toDtoList(): List<ParkingSpaceStatusDto> {
    return this.map { it.toDto() }
} 