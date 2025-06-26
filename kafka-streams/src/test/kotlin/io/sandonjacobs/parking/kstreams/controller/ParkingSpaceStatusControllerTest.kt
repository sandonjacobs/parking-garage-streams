package io.sandonjacobs.parking.kstreams.controller

import io.sandonjacobs.parking.kstreams.ParkingSpaceStatusQueryService
import io.sandonjacobs.parking.kstreams.TestConfig
import io.sandonjacobs.streaming.parking.status.ParkingSpaceStatus
import io.sandonjacobs.streaming.parking.status.SpaceStatus
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(ParkingSpaceStatusController::class)
@ContextConfiguration(classes = [TestConfig::class, ParkingSpaceStatusControllerTest.TestMockConfig::class])
class ParkingSpaceStatusControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var queryService: ParkingSpaceStatusQueryService

    @TestConfiguration
    open class TestMockConfig {
        
        @Bean
        @Primary
        open fun mockQueryService(): ParkingSpaceStatusQueryService {
            return mock(ParkingSpaceStatusQueryService::class.java)
        }
    }

    @Test
    fun `should return 404 for non-existent parking space`() {
        // Given
        val spaceId = "non-existent-space"
        `when`(queryService.getParkingSpaceStatus(spaceId)).thenReturn(null)

        // When & Then
        mockMvc.perform(get("/api/parking-spaces/$spaceId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return all parking spaces`() {
        // Given
        val mockStatuses = listOf<ParkingSpaceStatus>()
        `when`(queryService.getAllParkingSpaceStatuses()).thenReturn(mockStatuses)

        // When & Then
        mockMvc.perform(get("/api/parking-spaces"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should return occupied parking spaces`() {
        // Given
        val mockOccupiedStatuses = listOf<ParkingSpaceStatus>()
        `when`(queryService.getParkingSpaceStatusesByStatus(SpaceStatus.OCCUPIED)).thenReturn(mockOccupiedStatuses)

        // When & Then
        mockMvc.perform(get("/api/parking-spaces/occupied"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should return vacant parking spaces`() {
        // Given
        val mockVacantStatuses = listOf<ParkingSpaceStatus>()
        `when`(queryService.getParkingSpaceStatusesByStatus(SpaceStatus.VACANT)).thenReturn(mockVacantStatuses)

        // When & Then
        mockMvc.perform(get("/api/parking-spaces/vacant"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should return parking space counts`() {
        // Given
        val mockCounts = mapOf(SpaceStatus.OCCUPIED to 5, SpaceStatus.VACANT to 10)
        `when`(queryService.getParkingSpaceCountsByStatus()).thenReturn(mockCounts)

        // When & Then
        mockMvc.perform(get("/api/parking-spaces/counts"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should return total parking space count`() {
        // Given
        `when`(queryService.getTotalParkingSpaceCount()).thenReturn(15)

        // When & Then
        mockMvc.perform(get("/api/parking-spaces/count"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should return health status`() {
        // Given
        `when`(queryService.isStoreReady()).thenReturn(true)

        // When & Then
        mockMvc.perform(get("/api/parking-spaces/health"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.ready").exists())
            .andExpect(jsonPath("$.status").exists())
    }

    @Test
    fun `should return 400 for invalid status filter`() {
        // When & Then
        mockMvc.perform(get("/api/parking-spaces/status/invalid-status"))
            .andExpect(status().isBadRequest)
    }
} 