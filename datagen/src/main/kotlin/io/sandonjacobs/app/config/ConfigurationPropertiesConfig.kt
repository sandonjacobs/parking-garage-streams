package io.sandonjacobs.app.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ParkingGarageConfigurationLoader::class)
open class ConfigurationPropertiesConfig 