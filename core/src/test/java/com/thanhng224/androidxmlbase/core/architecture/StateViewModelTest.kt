package com.thanhng224.androidxmlbase.core.architecture

import app.cash.turbine.test
import com.thanhng224.androidxmlbase.core.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private data class CounterState(
    val value: Int = 0,
) : UiState

private sealed interface CounterEvent : UiEvent {
    data object Increment : CounterEvent
}

private sealed interface CounterEffect : UiEffect {
    data class Announce(
        val text: String,
    ) : CounterEffect
}

private class CounterViewModel : StateViewModel<CounterState, CounterEvent, CounterEffect>(CounterState()) {
    override fun onEvent(event: CounterEvent) {
        when (event) {
            is CounterEvent.Increment -> {
                setState { copy(value = value + 1) }
                sendEffect(CounterEffect.Announce("incremented to ${currentState.value}"))
            }
        }
    }
}

class StateViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `setState updates the exposed state flow`() =
        runTest {
            val viewModel = CounterViewModel()

            viewModel.state.test {
                assertEquals(0, awaitItem().value)
                viewModel.onEvent(CounterEvent.Increment)
                assertEquals(1, awaitItem().value)
            }
        }

    @Test
    fun `sendEffect emits a one-shot effect`() =
        runTest {
            val viewModel = CounterViewModel()

            viewModel.effect.test {
                viewModel.onEvent(CounterEvent.Increment)
                assertEquals(CounterEffect.Announce("incremented to 1"), awaitItem())
            }
        }
}
