package io.sandonjacobs.app.dto

import io.sandonjacobs.streaming.parking.model.ParkingEvent

data class ParkingEventDto(
    val type: String,
    val space: ParkingSpaceDto,
    val vehicle: VehicleDto,
    val timestamp: Long
) {
    companion object {
        fun fromParkingEvent(event: ParkingEvent): ParkingEventDto {
            return ParkingEventDto(
                type = event.type.name,
                space = event.space.toDto(),
                vehicle = event.vehicle.toDto(),
                timestamp = event.timestamp.seconds * 1000 + event.timestamp.nanos / 1_000_000
            )
        }
    }
} 