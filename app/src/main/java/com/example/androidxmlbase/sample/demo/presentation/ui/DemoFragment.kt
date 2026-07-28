package com.example.androidxmlbase.sample.demo.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.androidxmlbase.R
import com.example.androidxmlbase.databinding.FragmentDemoBinding
import com.example.androidxmlbase.sample.demo.domain.model.DemoWeather
import com.example.androidxmlbase.sample.demo.presentation.state.DemoUiEffect
import com.example.androidxmlbase.sample.demo.presentation.state.DemoUiEvent
import com.example.androidxmlbase.sample.demo.presentation.state.DemoWeatherError
import com.example.androidxmlbase.sample.demo.presentation.state.DemoWeatherState
import com.example.androidxmlbase.sample.demo.presentation.viewmodel.DemoViewModel
import com.thanhng224.androidxmlbase.core.ui.base.BaseFragment
import com.thanhng224.androidxmlbase.core.ui.base.setOnDebouncedClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DemoFragment : BaseFragment<FragmentDemoBinding>() {
    private val viewModel: DemoViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentDemoBinding = FragmentDemoBinding.inflate(inflater, container, false)

    override fun onBindingReady(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        // Increment is the button most likely to be rapid-tapped, so it's the one debounced.
        binding.btnIncrement.setOnDebouncedClickListener {
            viewModel.onEvent(DemoUiEvent.IncrementClicked)
        }
        binding.btnRefreshWeather.setOnDebouncedClickListener {
            viewModel.onEvent(DemoUiEvent.RefreshWeatherClicked)
        }

        observeState()
        observeEffects()
    }

    private fun observeState() {
        viewModel.state.collectOnStarted { state ->
            binding.tvCount.text = getString(R.string.demo_count_format, state.count)
            binding.tvWeather.text = state.weather.toDisplayText()
        }
    }

    private fun DemoWeatherState.toDisplayText(): String =
        when (this) {
            DemoWeatherState.Loading -> getString(R.string.demo_weather_loading)
            is DemoWeatherState.Success -> weather.toDisplayText()
            is DemoWeatherState.Error -> reason.toDisplayText()
        }

    private fun DemoWeather.toDisplayText(): String =
        getString(
            R.string.demo_weather_summary,
            weatherCode.toDisplayText(),
            temperatureCelsius,
            apparentTemperatureCelsius,
            windSpeedKph,
        )

    private fun Int.toDisplayText(): String =
        when (this) {
            0 -> getString(R.string.demo_weather_clear_sky)
            1 -> getString(R.string.demo_weather_mainly_clear)
            2 -> getString(R.string.demo_weather_partly_cloudy)
            3 -> getString(R.string.demo_weather_overcast)
            45,
            48,
            -> getString(R.string.demo_weather_fog)
            51,
            53,
            55,
            56,
            57,
            -> getString(R.string.demo_weather_drizzle)
            61,
            63,
            65,
            66,
            67,
            80,
            81,
            82,
            -> getString(R.string.demo_weather_rain)
            71,
            73,
            75,
            77,
            85,
            86,
            -> getString(R.string.demo_weather_snow)
            95,
            96,
            99,
            -> getString(R.string.demo_weather_thunderstorm)
            else -> getString(R.string.demo_weather_unknown)
        }

    private fun DemoWeatherError.toDisplayText(): String =
        when (this) {
            DemoWeatherError.SERVER -> getString(R.string.demo_weather_error_server)
            DemoWeatherError.NO_CONNECTION -> getString(R.string.demo_weather_error_no_connection)
            DemoWeatherError.UNEXPECTED_RESPONSE -> getString(R.string.demo_weather_error_unexpected_response)
            DemoWeatherError.EMPTY_RESPONSE -> getString(R.string.demo_weather_error_empty_response)
        }

    private fun observeEffects() {
        viewModel.effect.collectOnStarted { effect ->
            when (effect) {
                DemoUiEffect.ShowMaxCountReached ->
                    Toast
                        .makeText(
                            requireContext(),
                            getString(R.string.demo_max_count_reached),
                            Toast.LENGTH_SHORT,
                        ).show()
            }
        }
    }
}
