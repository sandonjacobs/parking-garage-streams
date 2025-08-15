package io.sandonjacobs.parking.kstreams

import com.google.protobuf.Timestamp
import io.sandonjacobs.streaming.parking.model.ParkingEvent
import io.sandonjacobs.streaming.parking.model.ParkingEventType
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import io.sandonjacobs.streaming.parking.status.SpaceStatus
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Kafka Streams topology for processing parking events and maintaining parking space status.
 */
class ParkingSpaceStatusTopology(
    private val parkingEventSerde: Serde<ParkingEvent>,
    private val parkingSpaceStatusSerde: Serde<ParkingSpaceStatus>
) {

    private val logger = LoggerFactory.getLogger(ParkingSpaceStatusTopology::class.java)

    companion object {
        const val PARKING_EVENTS_TOPIC = "parking-events"
        const val PARKING_SPACE_STATUS_TOPIC = "parking-space-status"
    }

    /**
     * Builds the Kafka Streams topology.
     */
    fun buildTopology(builder: StreamsBuilder): Topology {

        // Stream from parking-events topic
        val parkingEventsStream = builder.stream(
            PARKING_EVENTS_TOPIC,
            Consumed.with(Serdes.String(), parkingEventSerde)
        )

        // KTable with the latest known status for each space
        val spaceStatusTable = builder.table(
            PARKING_SPACE_STATUS_TOPIC,
            Consumed.with(Serdes.String(), parkingSpaceStatusSerde)
        )

        // Process parking events using current state to gate EXIT events
        parkingEventsStream
            .peek { _, event -> logger.debug("incoming parking event for space -> {}", event.space) }
            .leftJoin(spaceStatusTable) { event, currentStatus -> event to currentStatus }
            .filter { key, (event, currentStatus) ->
                when (event.type) {
                    ParkingEventType.ENTER -> {
                        val canEnter = currentStatus?.status != SpaceStatus.OCCUPIED
                        if (!canEnter) {
                            logger.debug(
                                "dropping ENTER for space {} because current status is {}",
                                key,
                                currentStatus?.status
                            )
                        }
                        canEnter
                    }
                    ParkingEventType.EXIT -> {
                        val isOccupied = currentStatus?.status == SpaceStatus.OCCUPIED
                        if (!isOccupied) {
                            logger.debug(
                                "dropping EXIT for space {} because current status is {}",
                                key,
                                currentStatus?.status
                            )
                        }
                        isOccupied
                    }
                    else -> true
                }
            }
            .mapValues { _, (event, _) ->
                when (event.type) {
                    ParkingEventType.ENTER -> {
                        logger.debug("space {} being marked as OCCUPIED", event.space)
                        event.mkStatus(SpaceStatus.OCCUPIED)
                    }
                    ParkingEventType.EXIT -> {
                        logger.debug("space {} being marked as VACANT due to EXIT", event.space)
                        event.mkStatus(SpaceStatus.VACANT)
                    }
                    else -> {
                        logger.debug(
                            "space {} being marked as VACANT due to event type {}",
                            event.space,
                            event.type
                        )
                        event.mkStatus(SpaceStatus.VACANT)
                    }
                }
            }
            .peek { _, status -> logger.debug("emitting status for space {} -> {}", status.space.id, status.status) }
            .to(
                PARKING_SPACE_STATUS_TOPIC,
                Produced.with(Serdes.String(), parkingSpaceStatusSerde)
            )

        logger.info("Built ParkingSpaceStatus topology")
        return builder.build()
    }

    fun ParkingEvent.mkStatus(status: SpaceStatus): ParkingSpaceStatus {
        val now = Instant.now()

        return ParkingSpaceStatus.newBuilder()
            .setId(this.space.id)
            .setSpace(this.space)
            .setStatus(status)
            .setLastUpdated(
                Timestamp.newBuilder()
                    .setSeconds(now.epochSecond)
                    .setNanos(now.nano)
                    .build()
            )
            .apply {
                if (status == SpaceStatus.OCCUPIED) {
                    setVehicle(this@mkStatus.vehicle)
                }
            }
            .build()

    }

}
