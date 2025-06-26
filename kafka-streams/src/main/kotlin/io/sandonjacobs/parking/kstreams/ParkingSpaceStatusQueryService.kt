package io.sandonjacobs.parking.kstreams

import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.stereotype.Service

/**
 * Service for querying parking space status from the materialized state store.
 * This service runs within the same Kafka Streams application instance.
 */
@Service
open class ParkingSpaceStatusQueryService(
    private val streamsBuilderFactoryBean: StreamsBuilderFactoryBean) {

    private val logger = LoggerFactory.getLogger(ParkingSpaceStatusQueryService::class.java)

    companion object {
        const val PARKING_SPACE_STATUS_STORE = "parking-space-status-store"
    }

    /**
     * Gets the current status of a specific parking space by its ID.
     *
     * @param spaceId The ID of the parking space
     * @return The current ParkingSpaceStatus, or null if not found
     */
    fun getParkingSpaceStatus(spaceId: String): ParkingSpaceStatus? {
        return try {
            val store = getStore()
            store.get(spaceId)
        } catch (e: Exception) {
            logger.error("Error querying parking space status for spaceId: {}", spaceId, e)
            null
        }
    }

    /**
     * Gets all parking space statuses currently in the store.
     *
     * @return List of all ParkingSpaceStatus entries
     */
    fun getAllParkingSpaceStatuses(): List<ParkingSpaceStatus> {
        return try {
            val store = getStore()
            val iterator = store.all()
            val statuses = mutableListOf<ParkingSpaceStatus>()
            
            while (iterator.hasNext()) {
                val entry = iterator.next()
                entry.value?.let { statuses.add(it) }
            }
            iterator.close()
            statuses
        } catch (e: Exception) {
            logger.error("Error querying all parking space statuses", e)
            emptyList()
        }
    }

    /**
     * Gets parking space statuses filtered by status (OCCUPIED or VACANT).
     *
     * @param status The SpaceStatus to filter by
     * @return List of ParkingSpaceStatus entries matching the filter
     */
    fun getParkingSpaceStatusesByStatus(status: io.sandonjacobs.streaming.parking.status.SpaceStatus): List<ParkingSpaceStatus> {
        return getAllParkingSpaceStatuses().filter { it.status == status }
    }

    /**
     * Gets the count of parking spaces by status.
     *
     * @return Map of SpaceStatus to count
     */
    fun getParkingSpaceCountsByStatus(): Map<io.sandonjacobs.streaming.parking.status.SpaceStatus, Int> {
        return getAllParkingSpaceStatuses()
            .groupBy { it.status }
            .mapValues { it.value.size }
    }

    /**
     * Gets the total number of parking spaces in the store.
     *
     * @return Total count of parking spaces
     */
    fun getTotalParkingSpaceCount(): Int {
        return try {
            val store = getStore()
            store.approximateNumEntries().toInt()
        } catch (e: Exception) {
            logger.error("Error getting total parking space count", e)
            0
        }
    }

    /**
     * Checks if the state store is ready for queries.
     *
     * @return true if the store is ready, false otherwise
     */
    fun isStoreReady(): Boolean {
        return try {
            val kafkaStreams = streamsBuilderFactoryBean.kafkaStreams
            kafkaStreams?.state() == KafkaStreams.State.RUNNING
        } catch (e: Exception) {
            logger.error("Error checking store readiness", e)
            false
        }
    }

    private fun getStore(): ReadOnlyKeyValueStore<String, ParkingSpaceStatus> {
        val kafkaStreams = streamsBuilderFactoryBean.kafkaStreams
            ?: throw IllegalStateException("KafkaStreams is not available")
            
        return kafkaStreams.store(
            StoreQueryParameters.fromNameAndType(
                PARKING_SPACE_STATUS_STORE,
                QueryableStoreTypes.keyValueStore()
            )
        )
    }
} 