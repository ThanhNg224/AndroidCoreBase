package com.example.androidcorebase.sample.demo.data.repository

import com.example.androidcorebase.sample.demo.data.datasource.DemoRemoteDataSource
import com.example.androidcorebase.sample.demo.data.mapper.toWeatherResult
import com.example.androidcorebase.sample.demo.domain.model.WeatherResult
import com.example.androidcorebase.sample.demo.domain.repository.DemoRepository
import com.thanhng224.androidcorebase.core.foundation.SettingsKey
import com.thanhng224.androidcorebase.core.foundation.SettingsStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DemoRepositoryImpl
    @Inject
    constructor(
        private val settingsStore: SettingsStore,
        private val remoteDataSource: DemoRemoteDataSource,
    ) : DemoRepository {
        override fun observeCount(): Flow<Int> = settingsStore.observe(DEMO_COUNTER_COUNT)

        override suspend fun saveCount(count: Int) {
            settingsStore.set(DEMO_COUNTER_COUNT, count)
        }

        override suspend fun fetchWeather(): WeatherResult = remoteDataSource.fetchCurrentWeather().toWeatherResult()

        private companion object {
            val DEMO_COUNTER_COUNT = SettingsKey.IntKey(name = "demo_counter_count", defaultValue = 0)
        }
    }
