package com.thanhng224.androidxmlbase.core.architecture.result

import com.thanhng224.androidxmlbase.core.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultStateTest {
    @Test
    fun `fold invokes onSuccess branch for Success`() {
        val result: ResultState<Int> = ResultState.Success(42)

        val text =
            result.fold(
                onLoading = { "loading" },
                onSuccess = { "success:$it" },
                onError = { message, _ -> "error:$message" },
            )

        assertEquals("success:42", text)
    }

    @Test
    fun `fold invokes onError branch for Error`() {
        val result: ResultState<Int> = ResultState.Error(UiText.DynamicString("boom"))

        val text =
            result.fold(
                onLoading = { "loading" },
                onSuccess = { "success:$it" },
                onError = { message, _ -> "error:${(message as UiText.DynamicString).value}" },
            )

        assertEquals("error:boom", text)
    }

    @Test
    fun `fold invokes onLoading branch for Loading`() {
        val result: ResultState<Int> = ResultState.Loading

        val text =
            result.fold(
                onLoading = { "loading" },
                onSuccess = { "success:$it" },
                onError = { message, _ -> "error:$message" },
            )

        assertEquals("loading", text)
    }
}
