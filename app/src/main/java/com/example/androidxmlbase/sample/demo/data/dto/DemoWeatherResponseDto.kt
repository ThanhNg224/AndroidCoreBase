package com.example.androidxmlbase.sample.demo.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DemoWeatherResponseDto(
    val current: DemoCurrentWeatherDto,
)

@Serializable
data class DemoCurrentWeatherDto(
    @SerialName("temperature_2m")
    val temperatureCelsius: Double,
    @SerialName("apparent_temperature")
    val apparentTemperatureCelsius: Double,
    @SerialName("weather_code")
    val weatherCode: Int,
    @SerialName("wind_speed_10m")
    val windSpeedKph: Double,
)
