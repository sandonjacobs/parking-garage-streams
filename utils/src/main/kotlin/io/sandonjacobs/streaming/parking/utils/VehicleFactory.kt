package io.sandonjacobs.streaming.parking.utils

import io.sandonjacobs.streaming.parking.model.*

/**
 * Factory class for creating Vehicle instances with realistic data.
 */
object VehicleFactory {
    
    private val states = listOf(
        "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA",
        "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD",
        "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ",
        "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "RI", "SC",
        "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"
    )
    
    private val vehicleTypeDistribution = mapOf(
        VehicleType.DEFAULT to 0.85,      // 85% default vehicles
        VehicleType.HANDICAP to 0.09,     // 9% handicap vehicles
        VehicleType.MOTORCYCLE to 0.06    // 6% motorcycles
    )
    
    /**
     * Creates a random vehicle with realistic license plate and type.
     */
    fun createRandomVehicle(): Vehicle {
        val vehicleType = selectRandomVehicleType()
        val licensePlate = generateLicensePlate(vehicleType)
        val state = states.random()
        
        return Vehicle.newBuilder()
            .setId(generateVehicleId())
            .setLicensePlate(licensePlate)
            .setState(state)
            .setType(vehicleType)
            .build()
    }
    
    /**
     * Creates a vehicle with a specific type.
     */
    fun createVehicle(vehicleType: VehicleType): Vehicle {
        val licensePlate = generateLicensePlate(vehicleType)
        val state = states.random()
        
        return Vehicle.newBuilder()
            .setId(generateVehicleId())
            .setLicensePlate(licensePlate)
            .setState(state)
            .setType(vehicleType)
            .build()
    }
    
    /**
     * Selects a random vehicle type based on distribution.
     */
    private fun selectRandomVehicleType(): VehicleType {
        val random = Math.random()
        var cumulative = 0.0
        
        for ((type, probability) in vehicleTypeDistribution) {
            cumulative += probability
            if (random <= cumulative) {
                return type
            }
        }
        
        return VehicleType.DEFAULT
    }
    
    /**
     * Generates a realistic license plate based on vehicle type.
     */
    private fun generateLicensePlate(vehicleType: VehicleType): String {
        return when (vehicleType) {
            VehicleType.HANDICAP -> {
                // Handicap plates often have special formatting
                val prefix = listOf("HC", "DP", "HP").random()
                val numbers = (1000..9999).random().toString()
                "$prefix$numbers"
            }
            VehicleType.MOTORCYCLE -> {
                // Motorcycle plates are typically shorter
                val letters = ('A'..'Z').random().toString() + ('A'..'Z').random().toString()
                val numbers = (10..99).random().toString()
                "$letters$numbers"
            }
            VehicleType.DEFAULT -> {
                // Standard format: 3 letters + 3 numbers
                val letters = (1..3).map { ('A'..'Z').random() }.joinToString("")
                val numbers = (100..999).random().toString()
                "$letters$numbers"
            }
            VehicleType.UNRECOGNIZED -> {
                // Fallback for unrecognized types - use default format
                val letters = (1..3).map { ('A'..'Z').random() }.joinToString("")
                val numbers = (100..999).random().toString()
                "$letters$numbers"
            }
        }
    }
    
    /**
     * Generates a unique vehicle ID.
     */
    private fun generateVehicleId(): String {
        return "vehicle-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
} 