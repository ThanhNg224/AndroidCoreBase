package com.example.androidxmlbase.sample.demo.data.datasource

import com.example.androidxmlbase.sample.demo.data.dto.DemoWeatherResponseDto
import com.thanhng224.androidxmlbase.core.network.ApiClient
import com.thanhng224.androidxmlbase.core.network.ApiResult
import javax.inject.Inject

interface DemoRemoteDataSource {
    suspend fun fetchCurrentWeather(): ApiResult<DemoWeatherResponseDto>
}

class DemoRemoteDataSourceImpl
    @Inject
    constructor(
        private val apiService: DemoApiService,
        private val apiClient: ApiClient,
    ) : DemoRemoteDataSource {
        override suspend fun fetchCurrentWeather(): ApiResult<DemoWeatherResponseDto> =
            apiClient.execute {
                apiService.getCurrentWeather(
                    latitude = HO_CHI_MINH_LATITUDE,
                    longitude = HO_CHI_MINH_LONGITUDE,
                    current = CURRENT_WEATHER_VARIABLES,
                    timezone = HO_CHI_MINH_TIMEZONE,
                )
            }

        private companion object {
            const val HO_CHI_MINH_LATITUDE = 10.8231
            const val HO_CHI_MINH_LONGITUDE = 106.6297
            const val CURRENT_WEATHER_VARIABLES =
                "temperature_2m,apparent_temperature,weather_code,wind_speed_10m"
            const val HO_CHI_MINH_TIMEZONE = "Asia/Ho_Chi_Minh"
        }
    }
