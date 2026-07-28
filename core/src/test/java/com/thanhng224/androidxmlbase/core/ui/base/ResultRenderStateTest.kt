package com.thanhng224.androidxmlbase.core.ui.base

import com.thanhng224.androidxmlbase.core.architecture.result.ResultState
import com.thanhng224.androidxmlbase.core.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultRenderStateTest {
    @Test
    fun `Loading maps to loading-visible render state`() {
        val result: ResultState<String> = ResultState.Loading

        val renderState = result.toRenderState()

        assertEquals(
            ResultRenderState(
                isLoadingVisible = true,
                isContentVisible = false,
                isErrorVisible = false,
                errorMessage = null,
            ),
            renderState,
        )
    }

    @Test
    fun `Success maps to content-visible render state`() {
        val result: ResultState<String> = ResultState.Success("hello")

        val renderState = result.toRenderState()

        assertEquals(
            ResultRenderState(
                isLoadingVisible = false,
                isContentVisible = true,
                isErrorVisible = false,
                errorMessage = null,
            ),
            renderState,
        )
    }

    @Test
    fun `Error maps to error-visible render state and passes the message through`() {
        val message = UiText.DynamicString("boom")
        val result: ResultState<String> = ResultState.Error(message, cause = IllegalStateException("boom"))

        val renderState = result.toRenderState()

        assertEquals(
            ResultRenderState(
                isLoadingVisible = false,
                isContentVisible = false,
                isErrorVisible = true,
                errorMessage = message,
            ),
            renderState,
        )
    }
}
