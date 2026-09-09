package com.example.androidcorebase.sample.demo.presentation.viewmodel

import app.cash.turbine.test
import com.example.androidcorebase.sample.demo.domain.model.DemoWeather
import com.example.androidcorebase.sample.demo.domain.model.WeatherError
import com.example.androidcorebase.sample.demo.domain.model.WeatherResult
import com.example.androidcorebase.sample.demo.domain.repository.DemoRepository
import com.example.androidcorebase.sample.demo.domain.usecase.FetchDemoWeatherUseCase
import com.example.androidcorebase.sample.demo.domain.usecase.IncrementCounterUseCase
import com.example.androidcorebase.sample.demo.domain.usecase.ObserveDemoCountUseCase
import com.example.androidcorebase.sample.demo.domain.usecase.SaveDemoCountUseCase
import com.example.androidcorebase.sample.demo.presentation.state.DemoMessageAction
import com.example.androidcorebase.sample.demo.presentation.state.DemoUiEvent
import com.example.androidcorebase.sample.demo.presentation.state.DemoWeatherError
import com.example.androidcorebase.sample.demo.presentation.state.DemoWeatherState
import com.thanhng224.androidcorebase.core.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class DemoViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeDemoRepository(
        initialCount: Int = 0,
        private val weatherResult: WeatherResult = WeatherResult.Success(DEMO_WEATHER),
    ) : DemoRepository {
        private val countFlow = MutableStateFlow(initialCount)
        val savedCounts = mutableListOf<Int>()

        override fun observeCount(): Flow<Int> = countFlow

        override suspend fun saveCount(count: Int) {
            savedCounts.add(count)
            countFlow.value = count
        }

        override suspend fun fetchWeather(): WeatherResult = weatherResult
    }

    /**
     * Unlike [FakeDemoRepository], [fetchWeather] here genuinely suspends until
     * [releaseWeatherFetch] is called, mimicking a real network call that hasn't resolved yet.
     */
    private class DeferredWeatherFakeDemoRepository(
        initialCount: Int = 0,
        private val weatherResult: WeatherResult,
    ) : DemoRepository {
        private val messageGate = CompletableDeferred<Unit>()
        private val countFlow = MutableStateFlow(initialCount)

        fun releaseWeatherFetch() {
            messageGate.complete(Unit)
        }

        override fun observeCount(): Flow<Int> = countFlow

        override suspend fun saveCount(count: Int) {
            countFlow.value = count
        }

        override suspend fun fetchWeather(): WeatherResult {
            messageGate.await()
            return weatherResult
        }
    }

    /**
     * Unlike [FakeDemoRepository], [observeCount] here genuinely suspends until
     * [releaseInitialLoad] is called, mimicking real DataStore's first read requiring an
     * actual suspension instead of resolving synchronously.
     */
    private class DeferredFakeDemoRepository(
        initialCount: Int,
    ) : DemoRepository {
        private val initialLoadGate = CompletableDeferred<Unit>()
        private val countFlow = MutableStateFlow(initialCount)
        val savedCounts = mutableListOf<Int>()

        fun releaseInitialLoad() {
            initialLoadGate.complete(Unit)
        }

        override fun observeCount(): Flow<Int> =
            flow {
                initialLoadGate.await()
                emitAll(countFlow)
            }

        override suspend fun saveCount(count: Int) {
            savedCounts.add(count)
            countFlow.value = count
        }

        override suspend fun fetchWeather(): WeatherResult = WeatherResult.Success(DEMO_WEATHER)
    }

    private fun createViewModel(repository: DemoRepository): DemoViewModel =
        DemoViewModel(
            incrementCounter = IncrementCounterUseCase(),
            observeDemoCount = ObserveDemoCountUseCase(repository),
            saveDemoCount = SaveDemoCountUseCase(repository),
            fetchDemoWeather = FetchDemoWeatherUseCase(repository),
        )

    @Test
    fun `increment event increases the count in state`() =
        runTest {
            val viewModel = createViewModel(FakeDemoRepository())

            viewModel.state.test {
                assertEquals(0, awaitItem().count)
                viewModel.onEvent(DemoUiEvent.IncrementClicked)
                assertEquals(1, awaitItem().count)
            }
        }

    @Test
    fun `reaching the max count enqueues a pending message with a reset action`() =
        runTest {
            val viewModel = createViewModel(FakeDemoRepository())

            repeat(10) { viewModel.onEvent(DemoUiEvent.IncrementClicked) }

            val message = viewModel.state.value.pendingMessages.single()
            assertEquals(DemoMessageAction.ResetCounter, message.action)
        }

    @Test
    fun `messages remain FIFO and duplicate acknowledgement is ignored`() =
        runTest {
            val viewModel = createViewModel(FakeDemoRepository())
            viewModel.enqueueForTest(DemoMessageAction.ResetCounter)
            viewModel.enqueueForTest(null)
            val first = viewModel.state.value.pendingMessages.first()

            viewModel.onMessageHandled(first.id)
            viewModel.onMessageHandled(first.id)

            assertEquals(1, viewModel.state.value.pendingMessages.size)
            assertNotEquals(first.id, viewModel.state.value.pendingMessages.single().id)
        }

    @Test
    fun `onMessageAction removes the head before executing the reset action`() =
        runTest {
            val repository = FakeDemoRepository(initialCount = 5)
            val viewModel = createViewModel(repository)
            viewModel.enqueueForTest(DemoMessageAction.ResetCounter)
            val message = viewModel.state.value.pendingMessages.first()

            viewModel.onMessageAction(message.id)

            assertEquals(0, viewModel.state.value.pendingMessages.size)
            assertEquals(0, viewModel.state.value.count)
        }

    @Test
    fun `a stale message acknowledgement is a no-op`() =
        runTest {
            val viewModel = createViewModel(FakeDemoRepository())
            viewModel.enqueueForTest(null)
            val staleId = viewModel.state.value.pendingMessages.first().id
            viewModel.onMessageHandled(staleId)
            viewModel.enqueueForTest(null)

            viewModel.onMessageAction(staleId)

            assertEquals(1, viewModel.state.value.pendingMessages.size)
        }

    @Test
    fun `pending messages survive a new collector attaching later, config-change style`() =
        runTest {
            val viewModel = createViewModel(FakeDemoRepository())
            viewModel.enqueueForTest(null)

            viewModel.state.test {
                assertEquals(1, awaitItem().pendingMessages.size)
            }
        }

    @Test
    fun `initial state reflects the count already persisted in the repository`() =
        runTest {
            val viewModel = createViewModel(FakeDemoRepository(initialCount = 5))

            viewModel.state.test {
                assertEquals(5, awaitItem().count)
            }
        }

    @Test
    fun `incrementing saves the new count to the repository`() =
        runTest {
            val repository = FakeDemoRepository()
            val viewModel = createViewModel(repository)

            viewModel.state.test {
                awaitItem()
                viewModel.onEvent(DemoUiEvent.IncrementClicked)
                awaitItem()
            }

            assertEquals(listOf(1), repository.savedCounts)
        }

    @Test
    fun `count changes saved elsewhere in the repository are reflected in state`() =
        runTest {
            val repository = FakeDemoRepository()
            val viewModel = createViewModel(repository)

            viewModel.state.test {
                assertEquals(0, awaitItem().count)
                repository.saveCount(3)
                assertEquals(3, awaitItem().count)
            }
        }

    @Test
    fun `increment issued before the initial persisted count loads is ignored`() =
        runTest {
            val repository = DeferredFakeDemoRepository(initialCount = 5)
            val viewModel = createViewModel(repository)

            viewModel.state.test {
                // The initial DataStore read hasn't resolved yet, so state is still the default.
                assertEquals(0, awaitItem().count)

                viewModel.onEvent(DemoUiEvent.IncrementClicked)
                // Must be a no-op: no state change, and definitely no save of a value computed
                // from the stale default.
                expectNoEvents()
                assertEquals(emptyList<Int>(), repository.savedCounts)

                // The persisted value finally loads.
                repository.releaseInitialLoad()
                assertEquals(5, awaitItem().count)

                // A subsequent increment now behaves normally.
                viewModel.onEvent(DemoUiEvent.IncrementClicked)
                assertEquals(6, awaitItem().count)
            }

            assertEquals(listOf(6), repository.savedCounts)
        }

    @Test
    fun `weather state starts Loading then becomes the fetch result once it resolves`() =
        runTest {
            val repository = DeferredWeatherFakeDemoRepository(weatherResult = WeatherResult.Success(DEMO_WEATHER))
            val viewModel = createViewModel(repository)

            viewModel.state.test {
                assertEquals(DemoWeatherState.Loading, awaitItem().weather)

                repository.releaseWeatherFetch()
                assertEquals(DemoWeatherState.Success(DEMO_WEATHER), awaitItem().weather)
            }
        }

    @Test
    fun `weather state reflects a failed fetch`() =
        runTest {
            val error: WeatherResult = WeatherResult.Failure(WeatherError.Network(Throwable("network down")))
            val repository = DeferredWeatherFakeDemoRepository(weatherResult = error)
            val viewModel = createViewModel(repository)

            viewModel.state.test {
                assertEquals(DemoWeatherState.Loading, awaitItem().weather)

                repository.releaseWeatherFetch()
                assertEquals(DemoWeatherState.Error(DemoWeatherError.NO_CONNECTION), awaitItem().weather)
            }
        }

    private companion object {
        val DEMO_WEATHER =
            DemoWeather(
                temperatureCelsius = 31.8,
                apparentTemperatureCelsius = 37.0,
                weatherCode = 2,
                windSpeedKph = 11.0,
            )
    }
}
