package com.example.androidcorebase.sample.demo.domain.usecase

import com.example.androidcorebase.sample.demo.domain.model.WeatherResult
import com.example.androidcorebase.sample.demo.domain.repository.DemoRepository
import javax.inject.Inject

class FetchDemoWeatherUseCase
    @Inject
    constructor(
        private val repository: DemoRepository,
    ) {
        suspend operator fun invoke(): WeatherResult = repository.fetchWeather()
    }
