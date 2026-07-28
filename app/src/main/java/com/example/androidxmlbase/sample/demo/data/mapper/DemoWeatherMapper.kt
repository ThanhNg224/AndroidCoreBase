package com.example.androidxmlbase.sample.demo.data.mapper

import com.example.androidxmlbase.sample.demo.data.dto.DemoWeatherResponseDto
import com.example.androidxmlbase.sample.demo.domain.model.DemoWeather
import com.thanhng224.androidxmlbase.core.architecture.result.AppError
import com.thanhng224.androidxmlbase.core.architecture.result.DomainResult
import com.thanhng224.androidxmlbase.core.network.ApiResult

fun ApiResult<DemoWeatherResponseDto>.toDomainResult(): DomainResult<DemoWeather> =
    when (this) {
        is ApiResult.Success ->
            DomainResult.Success(
                DemoWeather(
                    temperatureCelsius = data.current.temperatureCelsius,
                    apparentTemperatureCelsius = data.current.apparentTemperatureCelsius,
                    weatherCode = data.current.weatherCode,
                    windSpeedKph = data.current.windSpeedKph,
                ),
            )
        is ApiResult.HttpError -> DomainResult.Error(AppError.Http(code = code, serverMessage = message))
        is ApiResult.NetworkError -> DomainResult.Error(AppError.Network(cause))
        is ApiResult.ParseError -> DomainResult.Error(AppError.Parse(cause))
        ApiResult.EmptyBody -> DomainResult.Error(AppError.EmptyBody)
    }
