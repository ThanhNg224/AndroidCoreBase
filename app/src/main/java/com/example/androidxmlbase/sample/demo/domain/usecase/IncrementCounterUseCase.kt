package com.example.androidxmlbase.sample.demo.domain.usecase

import javax.inject.Inject

class IncrementCounterUseCase
    @Inject
    constructor() {
        operator fun invoke(currentCount: Int): IncrementResult {
            if (currentCount >= MAX_COUNT) {
                return IncrementResult(count = currentCount, capped = true)
            }
            val nextCount = currentCount + 1
            return IncrementResult(count = nextCount, capped = nextCount == MAX_COUNT)
        }

        data class IncrementResult(
            val count: Int,
            val capped: Boolean,
        )

        private companion object {
            const val MAX_COUNT = 10
        }
    }
