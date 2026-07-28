package com.example.androidxmlbase.sample.demo.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class IncrementCounterUseCaseTest {
    private val useCase = IncrementCounterUseCase()

    @Test
    fun `increments count by one when below the max`() {
        val result = useCase(currentCount = 0)

        assertEquals(1, result.count)
        assertEquals(false, result.capped)
    }

    @Test
    fun `reports capped when reaching the max count`() {
        val result = useCase(currentCount = 9)

        assertEquals(10, result.count)
        assertEquals(true, result.capped)
    }

    @Test
    fun `does not increment beyond the max count`() {
        val result = useCase(currentCount = 10)

        assertEquals(10, result.count)
        assertEquals(true, result.capped)
    }
}
