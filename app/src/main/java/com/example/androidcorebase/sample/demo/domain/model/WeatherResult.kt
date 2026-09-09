package com.example.androidcorebase.sample.demo.domain.model

sealed interface WeatherError {
    data class Server(
        val code: Int,
        val message: String,
    ) : WeatherError

    data class Network(
        val cause: Throwable,
    ) : WeatherError

    data class Parse(
        val cause: Throwable,
    ) : WeatherError

    data object EmptyBody : WeatherError
}

sealed interface WeatherResult {
    data class Success(
        val weather: DemoWeather,
    ) : WeatherResult

    data class Failure(
        val error: WeatherError,
    ) : WeatherResult
}
