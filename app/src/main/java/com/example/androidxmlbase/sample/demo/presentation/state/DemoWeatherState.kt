package com.example.androidxmlbase.sample.demo.presentation.state

import com.example.androidxmlbase.sample.demo.domain.model.DemoWeather

sealed interface DemoWeatherState {
    data object Loading : DemoWeatherState

    data class Success(
        val weather: DemoWeather,
    ) : DemoWeatherState

    data class Error(
        val reason: DemoWeatherError,
    ) : DemoWeatherState
}

enum class DemoWeatherError {
    SERVER,
    NO_CONNECTION,
    UNEXPECTED_RESPONSE,
    EMPTY_RESPONSE,
}
