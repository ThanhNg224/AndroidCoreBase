package com.example.androidcorebase.sample.demo.data.mapper

import com.example.androidcorebase.sample.demo.data.dto.DemoWeatherResponseDto
import com.example.androidcorebase.sample.demo.domain.model.DemoWeather
import com.example.androidcorebase.sample.demo.domain.model.WeatherError
import com.example.androidcorebase.sample.demo.domain.model.WeatherResult
import com.thanhng224.androidcorebase.core.network.ApiResult

fun ApiResult<DemoWeatherResponseDto>.toWeatherResult(): WeatherResult =
    when (this) {
        is ApiResult.Success ->
            WeatherResult.Success(
                DemoWeather(
                    temperatureCelsius = data.current.temperatureCelsius,
                    apparentTemperatureCelsius = data.current.apparentTemperatureCelsius,
                    weatherCode = data.current.weatherCode,
                    windSpeedKph = data.current.windSpeedKph,
                ),
            )
        is ApiResult.HttpError -> WeatherResult.Failure(WeatherError.Server(code = code, message = message))
        is ApiResult.NetworkError -> WeatherResult.Failure(WeatherError.Network(cause))
        is ApiResult.ParseError -> WeatherResult.Failure(WeatherError.Parse(cause))
        ApiResult.EmptyBody -> WeatherResult.Failure(WeatherError.EmptyBody)
    }
