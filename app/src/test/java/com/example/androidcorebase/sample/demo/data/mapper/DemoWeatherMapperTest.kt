package com.example.androidcorebase.sample.demo.data.mapper

import com.example.androidcorebase.sample.demo.data.dto.DemoCurrentWeatherDto
import com.example.androidcorebase.sample.demo.data.dto.DemoWeatherResponseDto
import com.example.androidcorebase.sample.demo.domain.model.DemoWeather
import com.example.androidcorebase.sample.demo.domain.model.WeatherError
import com.example.androidcorebase.sample.demo.domain.model.WeatherResult
import com.thanhng224.androidcorebase.core.network.ApiFailure
import com.thanhng224.androidcorebase.core.network.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class DemoWeatherMapperTest {
    private val networkCause = IOException("network down")
    private val parseCause = Throwable("malformed json")

    private data class Case(
        val name: String,
        val input: ApiResult<DemoWeatherResponseDto>,
        val expected: WeatherResult,
    )

    private val cases =
        listOf(
            Case(
                name = "success maps to a domain weather snapshot",
                input =
                    ApiResult.Success(
                        DemoWeatherResponseDto(
                            current =
                                DemoCurrentWeatherDto(
                                    temperatureCelsius = 31.8,
                                    apparentTemperatureCelsius = 37.0,
                                    weatherCode = 2,
                                    windSpeedKph = 11.0,
                                ),
                        ),
                    ),
                expected =
                    WeatherResult.Success(
                        DemoWeather(
                            temperatureCelsius = 31.8,
                            apparentTemperatureCelsius = 37.0,
                            weatherCode = 2,
                            windSpeedKph = 11.0,
                        ),
                    ),
            ),
            Case(
                name = "http failure maps to Server weather error",
                input = ApiResult.Failure(ApiFailure.Http(code = 404, serverMessage = "Not Found")),
                expected = WeatherResult.Failure(WeatherError.Server(code = 404, message = "Not Found")),
            ),
            Case(
                name = "network failure maps to Network weather error carrying the cause",
                input = ApiResult.Failure(ApiFailure.Network(networkCause)),
                expected = WeatherResult.Failure(WeatherError.Network(networkCause)),
            ),
            Case(
                name = "serialization failure maps to Parse weather error carrying the cause",
                input = ApiResult.Failure(ApiFailure.Serialization(parseCause)),
                expected = WeatherResult.Failure(WeatherError.Parse(parseCause)),
            ),
            Case(
                name = "empty body maps to EmptyBody weather error",
                input = ApiResult.Failure(ApiFailure.EmptyBody),
                expected = WeatherResult.Failure(WeatherError.EmptyBody),
            ),
        )

    @Test
    fun `toWeatherResult maps every ApiResult branch to the expected WeatherResult`() {
        cases.forEach { case ->
            assertEquals(case.name, case.expected, case.input.toWeatherResult())
        }
    }
}
