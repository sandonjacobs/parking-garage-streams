package io.sandonjacobs.streaming.parking.calculator

enum class CapacityType {
    ZONE, ROW
}

/**
 * Data class to hold capacity information for a parking structure entity
 */
data class CapacityData(
    val carCapacity: Int = 0,
    val handicapCapacity: Int = 0,
    val motorcycleCapacity: Int = 0,
    val type: CapacityType? = null  // Optional - only if you need to distinguish
)