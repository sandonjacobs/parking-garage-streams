package io.sandonjacobs.app.service

import io.sandonjacobs.app.kafka.ParkingEventProducer
import io.sandonjacobs.streaming.parking.model.*
import io.sandonjacobs.streaming.parking.utils.ParkingEventFactory
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Service
class ParkingEventGenerator(
    private val kafkaProducer: ParkingEventProducer
) {
    
    private val logger = LoggerFactory.getLogger(ParkingEventGenerator::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val runningGenerators = ConcurrentHashMap<String, Job>()
    
    /**
     * Starts generating parking events for a specific garage.
     * Each garage runs in its own coroutine with realistic timing patterns.
     */
    fun startGeneratingEvents(
        garage: ParkingGarage,
        eventsPerMinute: Int = 10
    ) {
        val garageId = garage.id
        
        // Cancel any existing generator for this garage
        runningGenerators[garageId]?.cancel()
        
        logger.info("Starting event generation for garage: $garageId")
        
        val job = scope.launch {
            try {
                generateEventsForGarage(garage, eventsPerMinute)
            } catch (e: CancellationException) {
                logger.info("Event generation cancelled for garage: $garageId")
                throw e // Re-throw cancellation exceptions
            } catch (e: Exception) {
                logger.error("Error generating events for garage $garageId", e)
            } finally {
                runningGenerators.remove(garageId)
                logger.info("Stopped event generation for garage: $garageId")
            }
        }
        
        runningGenerators[garageId] = job
    }
    
    /**
     * Stops generating events for a specific garage.
     */
    fun stopGeneratingEvents(garageId: String) {
        runningGenerators[garageId]?.cancel()
        logger.info("Requested stop for event generation in garage: $garageId")
    }
    
    /**
     * Stops all event generators.
     */
    fun stopAllGenerators() {
        runningGenerators.values.forEach { it.cancel() }
        logger.info("Requested stop for all event generators")
    }
    
    /**
     * Checks if event generation is running for a specific garage.
     */
    fun isGeneratingEvents(garageId: String): Boolean {
        return runningGenerators[garageId]?.isActive ?: false
    }
    
    /**
     * Gets the list of garages currently generating events.
     */
    fun getActiveGarages(): Set<String> {
        return runningGenerators.keys
    }
    
    /**
     * Gets the Kafka topic name being used for parking events.
     */
    fun getTopicName(): String {
        return kafkaProducer.getTopicName()
    }
    
    /**
     * Main event generation loop for a single garage.
     */
    private suspend fun generateEventsForGarage(
        garage: ParkingGarage,
        baseEventsPerMinute: Int
    ) {
        val garageId = garage.id
        val location = garage.location
        val allParkingSpaces = getAllParkingSpaces(garage)
        
        if (allParkingSpaces.isEmpty()) {
            logger.warn("No parking spaces found for garage: $garageId")
            return
        }
        
        logger.info("Starting event generation for garage $garageId with ${allParkingSpaces.size} parking spaces")
        
        try {
            while (true) {
                val currentTime = LocalTime.now()
                val timeMultiplier = getTimeMultiplier(currentTime)
                val eventsThisMinute = (baseEventsPerMinute * timeMultiplier).toInt()
                
                // Calculate delay between events
                val delayBetweenEvents = if (eventsThisMinute > 0) {
                    60000L / eventsThisMinute // milliseconds between events
                } else {
                    60000L // 1 minute if no events
                }
                
                // Add some randomness to the timing
                val actualDelay = (delayBetweenEvents * (0.5 + Random.nextDouble())).toLong()
                
                delay(actualDelay)
                
                // Select a random parking space
                val randomSpace = allParkingSpaces.random()
                
                // Generate a parking event
                val event = ParkingEventFactory.createRandomEvent(garageId, randomSpace, location)
                
                // Send the event to Kafka
                kafkaProducer.sendParkingEventSync(event)
                
                logger.debug("Generated ${event.type} event for garage $garageId, space ${event.space.id}, vehicle ${event.vehicle.licensePlate}")
            }
        } catch (e: CancellationException) {
            logger.info("Event generation cancelled for garage: $garageId")
            throw e // Re-throw cancellation exceptions
        } catch (e: Exception) {
            logger.error("Error in event generation loop for garage: $garageId", e)
            // Don't retry on general exceptions, just log and exit
        }
    }
    
    /**
     * Gets all parking spaces from a garage (flattened from zones and rows).
     */
    private fun getAllParkingSpaces(garage: ParkingGarage): List<ParkingSpace> {
        return garage.parkingZonesList.flatMap { zone ->
            zone.parkingRowsList.flatMap { row ->
                row.parkingSpacesList
            }
        }
    }
    
    /**
     * Calculates a time multiplier based on the current time to simulate realistic traffic patterns.
     * Higher values during peak hours, lower values during off-peak hours.
     */
    private fun getTimeMultiplier(currentTime: LocalTime): Double {
        val hour = currentTime.hour
        
        return when {
            // Morning rush hour (7-9 AM)
            hour in 7..9 -> 2.5
            // Lunch time (11 AM - 1 PM)
            hour in 11..13 -> 1.8
            // Afternoon rush hour (4-6 PM)
            hour in 16..18 -> 2.2
            // Evening (6-8 PM)
            hour in 18..20 -> 1.5
            // Late night (10 PM - 6 AM)
            hour in 22..23 || hour in 0..6 -> 0.3
            // Regular business hours
            hour in 9..16 -> 1.2
            // Early morning
            hour in 6..7 -> 0.8
            // Late evening
            hour in 20..22 -> 0.6
            else -> 1.0
        }
    }
    
    /**
     * Shuts down the coroutine scope and cancels all running generators.
     */
    fun shutdown() {
        stopAllGenerators()
        scope.cancel()
        logger.info("Shutdown ParkingEventGenerator")
    }
} 