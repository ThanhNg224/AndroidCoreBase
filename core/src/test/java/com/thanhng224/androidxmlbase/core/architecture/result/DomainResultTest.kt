package com.thanhng224.androidxmlbase.core.architecture.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DomainResultTest {
    @Test
    fun `map transforms the Success payload`() {
        val result: DomainResult<Int> = DomainResult.Success(21)

        val mapped = result.map { it * 2 }

        assertEquals(DomainResult.Success(42), mapped)
    }

    @Test
    fun `map passes Error through without invoking the transform`() {
        // DomainResult.Error is DomainResult<Nothing>, so covariance already makes it a
        // DomainResult<Int> here — no cast needed.
        val error: DomainResult<Int> = DomainResult.Error(AppError.EmptyBody)
        var transformCalls = 0

        val mapped =
            error.map {
                transformCalls++
                it * 2
            }

        assertSame(error, mapped)
        assertEquals(0, transformCalls)
    }

    @Test
    fun `map changes the payload type`() {
        val result: DomainResult<Int> = DomainResult.Success(7)

        val mapped: DomainResult<String> = result.map { "value:$it" }

        assertEquals(DomainResult.Success("value:7"), mapped)
    }

    @Test
    fun `Business carries a code and message and reports no cause`() {
        val error: AppError = AppError.Business(code = 4001, message = "insufficient balance")

        assertEquals(AppError.Business(4001, "insufficient balance"), error)
        assertEquals(null, error.cause)
    }
}
