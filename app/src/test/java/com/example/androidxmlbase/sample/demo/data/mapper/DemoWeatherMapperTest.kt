package com.example.androidxmlbase.sample.demo.data.mapper

import com.example.androidxmlbase.sample.demo.data.dto.DemoCurrentWeatherDto
import com.example.androidxmlbase.sample.demo.data.dto.DemoWeatherResponseDto
import com.example.androidxmlbase.sample.demo.domain.model.DemoWeather
import com.thanhng224.androidxmlbase.core.architecture.result.AppError
import com.thanhng224.androidxmlbase.core.architecture.result.DomainResult
import com.thanhng224.androidxmlbase.core.network.ApiResult
import org.junit.Assert.assertEquals
import org.junit.Test

class DemoWeatherMapperTest {
    private val networkCause = Throwable("network down")
    private val parseCause = Throwable("malformed json")

    private data class Case(
        val name: String,
        val input: ApiResult<DemoWeatherResponseDto>,
        val expected: DomainResult<DemoWeather>,
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
                    DomainResult.Success(
                        DemoWeather(
                            temperatureCelsius = 31.8,
                            apparentTemperatureCelsius = 37.0,
                            weatherCode = 2,
                            windSpeedKph = 11.0,
                        ),
                    ),
            ),
            Case(
                name = "http error maps to Http app error",
                input = ApiResult.HttpError(code = 404, message = "Not Found"),
                expected = DomainResult.Error(AppError.Http(code = 404, serverMessage = "Not Found")),
            ),
            Case(
                name = "network error maps to Network app error carrying the cause",
                input = ApiResult.NetworkError(networkCause),
                expected = DomainResult.Error(AppError.Network(networkCause)),
            ),
            Case(
                name = "parse error maps to Parse app error carrying the cause",
                input = ApiResult.ParseError(parseCause),
                expected = DomainResult.Error(AppError.Parse(parseCause)),
            ),
            Case(
                name = "empty body maps to EmptyBody app error",
                input = ApiResult.EmptyBody,
                expected = DomainResult.Error(AppError.EmptyBody),
            ),
        )

    @Test
    fun `toDomainResult maps every ApiResult branch to the expected DomainResult`() {
        cases.forEach { case ->
            assertEquals(case.name, case.expected, case.input.toDomainResult())
        }
    }
}
