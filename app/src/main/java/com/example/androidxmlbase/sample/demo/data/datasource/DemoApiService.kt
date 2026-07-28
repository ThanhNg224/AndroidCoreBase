package com.example.androidxmlbase.sample.demo.data.datasource

import com.example.androidxmlbase.sample.demo.data.dto.DemoWeatherResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface DemoApiService {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String,
        @Query("timezone") timezone: String,
    ): Response<DemoWeatherResponseDto>
}
