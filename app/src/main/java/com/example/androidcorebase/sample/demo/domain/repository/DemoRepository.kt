package com.example.androidcorebase.sample.demo.domain.repository

import com.example.androidcorebase.sample.demo.domain.model.DemoWeather
import com.thanhng224.androidcorebase.core.architecture.result.DomainResult
import kotlinx.coroutines.flow.Flow

interface DemoRepository {
    fun observeCount(): Flow<Int>

    suspend fun saveCount(count: Int)

    suspend fun fetchWeather(): DomainResult<DemoWeather>
}
