package com.example.androidxmlbase.sample.demo.domain.usecase

import com.example.androidxmlbase.sample.demo.domain.model.DemoWeather
import com.example.androidxmlbase.sample.demo.domain.repository.DemoRepository
import com.thanhng224.androidxmlbase.core.architecture.UseCase
import com.thanhng224.androidxmlbase.core.architecture.result.DomainResult
import javax.inject.Inject

class FetchDemoWeatherUseCase
    @Inject
    constructor(
        private val repository: DemoRepository,
    ) : UseCase<Unit, DomainResult<DemoWeather>> {
        override suspend fun invoke(params: Unit): DomainResult<DemoWeather> = repository.fetchWeather()
    }
