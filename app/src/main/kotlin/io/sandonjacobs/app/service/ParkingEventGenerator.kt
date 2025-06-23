package io.sandonjacobs.app.service

import io.sandonjacobs.streaming.parking.model.*
import io.sandonjacobs.streaming.parking.utils.ParkingEventFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

@Service
class ParkingEventGenerator {
    
    private val logger = LoggerFactory.getLogger(ParkingEventGenerator::class.java)
    private val executor = Executors.newCachedThreadPool()
    private val runningGenerators = ConcurrentHashMap<String, AtomicBoolean>()
    
    /**
     * Starts generating parking events for a specific garage.
     * Each garage runs in its own thread with realistic timing patterns.
     */
    fun startGeneratingEvents(
        garage: ParkingGarage,
        eventsPerMinute: Int = 10,
        onEventGenerated: (ParkingEvent) -> Unit
    ) {
        val garageId = garage.id
        val isRunning = AtomicBoolean(true)
        runningGenerators[garageId] = isRunning
        
        logger.info("Starting event generation for garage: $garageId")
        
        executor.submit {
            try {
                generateEventsForGarage(garage, eventsPerMinute, isRunning, onEventGenerated)
            } catch (e: Exception) {
                logger.error("Error generating events for garage $garageId", e)
            } finally {
                runningGenerators.remove(garageId)
                logger.info("Stopped event generation for garage: $garageId")
            }
        }
    }
    
    /**
     * Stops generating events for a specific garage.
     */
    fun stopGeneratingEvents(garageId: String) {
        runningGenerators[garageId]?.set(false)
        logger.info("Requested stop for event generation in garage: $garageId")
    }
    
    /**
     * Stops all event generators.
     */
    fun stopAllGenerators() {
        runningGenerators.values.forEach { it.set(false) }
        logger.info("Requested stop for all event generators")
    }
    
    /**
     * Checks if event generation is running for a specific garage.
     */
    fun isGeneratingEvents(garageId: String): Boolean {
        return runningGenerators[garageId]?.get() ?: false
    }
    
    /**
     * Gets the list of garages currently generating events.
     */
    fun getActiveGarages(): Set<String> {
        return runningGenerators.keys
    }
    
    /**
     * Main event generation loop for a single garage.
     */
    private fun generateEventsForGarage(
        garage: ParkingGarage,
        baseEventsPerMinute: Int,
        isRunning: AtomicBoolean,
        onEventGenerated: (ParkingEvent) -> Unit
    ) {
        val garageId = garage.id
        val location = garage.location
        val allParkingSpaces = getAllParkingSpaces(garage)
        
        if (allParkingSpaces.isEmpty()) {
            logger.warn("No parking spaces found for garage: $garageId")
            return
        }
        
        logger.info("Starting event generation for garage $garageId with ${allParkingSpaces.size} parking spaces")
        
        while (isRunning.get()) {
            try {
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
                
                Thread.sleep(actualDelay)
                
                if (isRunning.get()) {
                    // Select a random parking space
                    val randomSpace = allParkingSpaces.random()
                    
                    // Generate a parking event
                    val event = ParkingEventFactory.createRandomEvent(garageId, randomSpace, location)
                    
                    // Call the callback with the generated event
                    onEventGenerated(event)
                    
                    logger.debug("Generated ${event.type} event for garage $garageId, space ${event.space.id}, vehicle ${event.vehicle.licensePlate}")
                }
                
            } catch (e: InterruptedException) {
                logger.info("Event generation interrupted for garage: $garageId")
                break
            } catch (e: Exception) {
                logger.error("Error in event generation loop for garage: $garageId", e)
                Thread.sleep(5000) // Wait 5 seconds before retrying
            }
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
     * Shuts down the executor service.
     */
    fun shutdown() {
        stopAllGenerators()
        executor.shutdown()
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
} 