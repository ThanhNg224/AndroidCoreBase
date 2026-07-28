package com.example.androidxmlbase.sample.demo.domain.usecase

import com.example.androidxmlbase.sample.demo.domain.repository.DemoRepository
import com.thanhng224.androidxmlbase.core.architecture.UseCase
import javax.inject.Inject

class SaveDemoCountUseCase
    @Inject
    constructor(
        private val repository: DemoRepository,
    ) : UseCase<Int, Unit> {
        override suspend fun invoke(params: Int) {
            repository.saveCount(params)
        }
    }
