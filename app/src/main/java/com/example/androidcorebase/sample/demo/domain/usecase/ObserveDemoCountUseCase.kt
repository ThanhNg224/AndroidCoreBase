package com.example.androidcorebase.sample.demo.domain.usecase

import com.example.androidcorebase.sample.demo.domain.repository.DemoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDemoCountUseCase
    @Inject
    constructor(
        private val repository: DemoRepository,
    ) {
        operator fun invoke(): Flow<Int> = repository.observeCount()
    }
