package com.example.androidxmlbase.sample.demo.domain.repository

import com.example.androidxmlbase.sample.demo.domain.model.DemoWeather
import com.thanhng224.androidxmlbase.core.architecture.result.DomainResult
import kotlinx.coroutines.flow.Flow

interface DemoRepository {
    fun observeCount(): Flow<Int>

    suspend fun saveCount(count: Int)

    suspend fun fetchWeather(): DomainResult<DemoWeather>
}
