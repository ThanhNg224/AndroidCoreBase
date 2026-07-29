package com.example.androidcorebase.sample.demo.domain.usecase

import com.example.androidcorebase.sample.demo.domain.model.DemoWeather
import com.example.androidcorebase.sample.demo.domain.repository.DemoRepository
import com.thanhng224.androidcorebase.core.architecture.UseCase
import com.thanhng224.androidcorebase.core.architecture.result.DomainResult
import javax.inject.Inject

class FetchDemoWeatherUseCase
    @Inject
    constructor(
        private val repository: DemoRepository,
    ) : UseCase<Unit, DomainResult<DemoWeather>> {
        override suspend fun invoke(params: Unit): DomainResult<DemoWeather> = repository.fetchWeather()
    }
