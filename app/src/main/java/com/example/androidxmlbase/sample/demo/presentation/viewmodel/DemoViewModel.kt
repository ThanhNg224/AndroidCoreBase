package com.example.androidxmlbase.sample.demo.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.androidxmlbase.sample.demo.domain.model.DemoWeather
import com.example.androidxmlbase.sample.demo.domain.usecase.FetchDemoWeatherUseCase
import com.example.androidxmlbase.sample.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidxmlbase.sample.demo.domain.usecase.ObserveDemoCountUseCase
import com.example.androidxmlbase.sample.demo.domain.usecase.SaveDemoCountUseCase
import com.example.androidxmlbase.sample.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.sample.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.sample.demo.presentation.state.DemoUiState
import com.example.androidxmlbase.sample.demo.presentation.state.DemoWeatherError
import com.example.androidxmlbase.sample.demo.presentation.state.DemoWeatherState
import com.thanhng224.androidxmlbase.core.architecture.StateViewModel
import com.thanhng224.androidxmlbase.core.architecture.result.AppError
import com.thanhng224.androidxmlbase.core.architecture.result.DomainResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DemoViewModel
    @Inject
    constructor(
        private val incrementCounter: IncrementCounterUseCase,
        private val observeDemoCount: ObserveDemoCountUseCase,
        private val saveDemoCount: SaveDemoCountUseCase,
        private val fetchDemoWeather: FetchDemoWeatherUseCase,
    ) : StateViewModel<DemoUiState, DemoUiEvent, DemoUiEffect>(DemoUiState()) {
        private var isInitialCountLoaded = false

        init {
            viewModelScope.launch {
                val initialCount = observeDemoCount().first()
                setState { copy(count = initialCount) }
                isInitialCountLoaded = true
                observeDemoCount().drop(1).collect { count -> setState { copy(count = count) } }
            }
            refreshWeather()
        }

        override fun onEvent(event: DemoUiEvent) {
            when (event) {
                is DemoUiEvent.IncrementClicked -> onIncrementClicked()
                DemoUiEvent.RefreshWeatherClicked -> refreshWeather()
            }
        }

        private fun onIncrementClicked() {
            if (!isInitialCountLoaded) return
            val result = incrementCounter(currentState.count)
            setState { copy(count = result.count) }
            viewModelScope.launch { saveDemoCount(result.count) }
            if (result.capped) {
                sendEffect(DemoUiEffect.ShowMaxCountReached)
            }
        }

        private fun refreshWeather() {
            viewModelScope.launch {
                setState { copy(weather = DemoWeatherState.Loading) }
                val result = fetchDemoWeather(Unit)
                setState { copy(weather = result.toWeatherState()) }
            }
        }

        private fun DomainResult<DemoWeather>.toWeatherState(): DemoWeatherState =
            when (this) {
                is DomainResult.Success -> DemoWeatherState.Success(data)
                is DomainResult.Error -> DemoWeatherState.Error(error.toWeatherError())
            }

        private fun AppError.toWeatherError(): DemoWeatherError =
            when (this) {
                is AppError.Http -> DemoWeatherError.SERVER
                is AppError.Network -> DemoWeatherError.NO_CONNECTION
                is AppError.Parse -> DemoWeatherError.UNEXPECTED_RESPONSE
                AppError.EmptyBody -> DemoWeatherError.EMPTY_RESPONSE
            }
    }
