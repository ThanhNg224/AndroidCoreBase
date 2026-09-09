package com.example.androidcorebase.sample.demo.presentation.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidcorebase.R
import com.example.androidcorebase.sample.demo.domain.model.WeatherError
import com.example.androidcorebase.sample.demo.domain.model.WeatherResult
import com.example.androidcorebase.sample.demo.domain.usecase.FetchDemoWeatherUseCase
import com.example.androidcorebase.sample.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidcorebase.sample.demo.domain.usecase.ObserveDemoCountUseCase
import com.example.androidcorebase.sample.demo.domain.usecase.SaveDemoCountUseCase
import com.example.androidcorebase.sample.demo.presentation.state.DemoMessageAction
import com.example.androidcorebase.sample.demo.presentation.state.DemoUiEvent
import com.example.androidcorebase.sample.demo.presentation.state.DemoUiState
import com.example.androidcorebase.sample.demo.presentation.state.DemoWeatherError
import com.example.androidcorebase.sample.demo.presentation.state.DemoWeatherState
import com.example.androidcorebase.sample.demo.presentation.state.PendingDemoMessage
import com.thanhng224.androidcorebase.core.ui.text.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@HiltViewModel
class DemoViewModel
    @Inject
    constructor(
        private val incrementCounter: IncrementCounterUseCase,
        private val observeDemoCount: ObserveDemoCountUseCase,
        private val saveDemoCount: SaveDemoCountUseCase,
        private val fetchDemoWeather: FetchDemoWeatherUseCase,
    ) : ViewModel() {
        private var isInitialCountLoaded = false
        private val nextMessageId = AtomicLong(0)
        private val mutableState = MutableStateFlow(DemoUiState())
        val state: StateFlow<DemoUiState> = mutableState.asStateFlow()

        init {
            viewModelScope.launch {
                val initialCount = observeDemoCount().first()
                mutableState.update { it.copy(count = initialCount) }
                isInitialCountLoaded = true
                observeDemoCount().drop(1).collect { count -> mutableState.update { it.copy(count = count) } }
            }
            refreshWeather()
        }

        fun onEvent(event: DemoUiEvent) {
            when (event) {
                is DemoUiEvent.IncrementClicked -> onIncrementClicked()
                DemoUiEvent.RefreshWeatherClicked -> refreshWeather()
            }
        }

        fun onMessageHandled(id: Long) {
            removeHeadIfMatching(id)
        }

        fun onMessageAction(id: Long) {
            val action = mutableState.value.pendingMessages.firstOrNull { it.id == id }?.action
            if (!removeHeadIfMatching(id)) return
            when (action) {
                DemoMessageAction.ResetCounter -> resetCounter()
                null -> Unit
            }
        }

        /** Returns whether [id] matched the current head and was removed. */
        private fun removeHeadIfMatching(id: Long): Boolean {
            var removed = false
            mutableState.update { current ->
                val head = current.pendingMessages.firstOrNull()
                if (head?.id != id) {
                    current
                } else {
                    removed = true
                    current.copy(pendingMessages = current.pendingMessages.drop(1))
                }
            }
            return removed
        }

        private fun resetCounter() {
            mutableState.update { it.copy(count = 0) }
            viewModelScope.launch { saveDemoCount(0) }
        }

        private fun onIncrementClicked() {
            if (!isInitialCountLoaded) return
            val result = incrementCounter(mutableState.value.count)
            mutableState.update { it.copy(count = result.count) }
            viewModelScope.launch { saveDemoCount(result.count) }
            if (result.capped) {
                enqueueMessage(
                    text = UiText.StringResource(R.string.demo_max_count_reached),
                    actionLabel = UiText.StringResource(R.string.demo_reset_action),
                    action = DemoMessageAction.ResetCounter,
                )
            }
        }

        private fun enqueueMessage(
            text: UiText,
            actionLabel: UiText? = null,
            action: DemoMessageAction? = null,
        ) {
            val message =
                PendingDemoMessage(
                    id = nextMessageId.incrementAndGet(),
                    text = text,
                    actionLabel = actionLabel,
                    action = action,
                )
            mutableState.update { it.copy(pendingMessages = it.pendingMessages + message) }
        }

        @VisibleForTesting
        fun enqueueForTest(action: DemoMessageAction?) {
            enqueueMessage(text = UiText.DynamicString("test"), action = action)
        }

        private fun refreshWeather() {
            viewModelScope.launch {
                mutableState.update { it.copy(weather = DemoWeatherState.Loading) }
                val result = fetchDemoWeather()
                mutableState.update { it.copy(weather = result.toWeatherState()) }
            }
        }

        private fun WeatherResult.toWeatherState(): DemoWeatherState =
            when (this) {
                is WeatherResult.Success -> DemoWeatherState.Success(weather)
                is WeatherResult.Failure -> DemoWeatherState.Error(error.toWeatherError())
            }

        private fun WeatherError.toWeatherError(): DemoWeatherError =
            when (this) {
                is WeatherError.Server -> DemoWeatherError.SERVER
                is WeatherError.Network -> DemoWeatherError.NO_CONNECTION
                is WeatherError.Parse -> DemoWeatherError.UNEXPECTED_RESPONSE
                WeatherError.EmptyBody -> DemoWeatherError.EMPTY_RESPONSE
            }
    }
