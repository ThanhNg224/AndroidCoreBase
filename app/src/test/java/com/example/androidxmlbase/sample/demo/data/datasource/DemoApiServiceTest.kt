package com.example.androidxmlbase.sample.demo.data.datasource

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

class DemoApiServiceTest {
    private lateinit var server: MockWebServer
    private lateinit var service: DemoApiService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        service =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(JSON.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(DemoApiService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `current weather request uses Open-Meteo's documented query and decodes its payload`() =
        runTest {
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "current": {
                        "temperature_2m": 31.8,
                        "apparent_temperature": 37.0,
                        "weather_code": 2,
                        "wind_speed_10m": 11.0
                      }
                    }
                    """.trimIndent(),
                ),
            )

            val response =
                service.getCurrentWeather(
                    latitude = 10.8231,
                    longitude = 106.6297,
                    current = "temperature_2m,apparent_temperature,weather_code,wind_speed_10m",
                    timezone = "Asia/Ho_Chi_Minh",
                )

            val requestUrl = requireNotNull(server.takeRequest().requestUrl)
            assertEquals("/v1/forecast", requestUrl.encodedPath)
            assertEquals("10.8231", requestUrl.queryParameter("latitude"))
            assertEquals("106.6297", requestUrl.queryParameter("longitude"))
            assertEquals(
                "temperature_2m,apparent_temperature,weather_code,wind_speed_10m",
                requestUrl.queryParameter("current"),
            )
            assertEquals("Asia/Ho_Chi_Minh", requestUrl.queryParameter("timezone"))
            assertNotNull(response.body())
            assertEquals(31.8, response.body()?.current?.temperatureCelsius)
        }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
