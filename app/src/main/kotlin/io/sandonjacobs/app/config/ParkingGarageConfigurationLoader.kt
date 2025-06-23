package io.sandonjacobs.app.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "parking-garages")
data class ParkingGarageConfigurationLoader(
    var garages: List<ParkingGarageConfig> = emptyList()
) 