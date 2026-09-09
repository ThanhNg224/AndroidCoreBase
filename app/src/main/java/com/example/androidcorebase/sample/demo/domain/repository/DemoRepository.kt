package com.example.androidcorebase.sample.demo.domain.repository

import com.example.androidcorebase.sample.demo.domain.model.WeatherResult
import kotlinx.coroutines.flow.Flow

interface DemoRepository {
    fun observeCount(): Flow<Int>

    suspend fun saveCount(count: Int)

    suspend fun fetchWeather(): WeatherResult
}
