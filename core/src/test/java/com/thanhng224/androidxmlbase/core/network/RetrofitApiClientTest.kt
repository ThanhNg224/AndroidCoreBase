package com.thanhng224.androidxmlbase.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

@Serializable
data class TestDto(
    val value: String,
)

interface TestService {
    @GET("/")
    suspend fun get(): Response<TestDto>
}

class RetrofitApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var service: TestService
    private val apiClient: ApiClient = RetrofitApiClient()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        service = retrofit.create(TestService::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `200 with valid JSON body maps to Success`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{"value":"hello"}"""))

            val result = apiClient.execute { service.get() }

            assertEquals(ApiResult.Success(TestDto("hello")), result)
        }

    @Test
    fun `404 maps to HttpError with the response code`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

            val result = apiClient.execute { service.get() }

            assertTrue(result is ApiResult.HttpError)
            assertEquals(404, (result as ApiResult.HttpError).code)
        }

    @Test
    fun `500 maps to HttpError with the response code`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500).setBody("server error"))

            val result = apiClient.execute { service.get() }

            assertTrue(result is ApiResult.HttpError)
            assertEquals(500, (result as ApiResult.HttpError).code)
        }

    @Test
    fun `204 No Content maps to EmptyBody`() =
        runTest {
            // Retrofit special-cases 204/205 and returns a null body without invoking the
            // converter, which is the only way to reliably get a null response.body() out
            // of the JSON converter (an empty string is not valid JSON and would instead
            // surface as a ParseError).
            server.enqueue(MockResponse().setResponseCode(204))

            val result = apiClient.execute { service.get() }

            assertEquals(ApiResult.EmptyBody, result)
        }

    @Test
    fun `malformed JSON body maps to ParseError`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{"not-value": true"""))

            val result = apiClient.execute { service.get() }

            assertTrue(result is ApiResult.ParseError)
        }

    @Test
    fun `unreachable server maps to NetworkError`() =
        runTest {
            val unreachableUrl = server.url("/")
            server.shutdown()

            val retrofit =
                Retrofit
                    .Builder()
                    .baseUrl(unreachableUrl)
                    .client(
                        OkHttpClient
                            .Builder()
                            .connectTimeout(500, TimeUnit.MILLISECONDS)
                            .readTimeout(500, TimeUnit.MILLISECONDS)
                            .build(),
                    ).addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
                    .build()
            val unreachableService = retrofit.create(TestService::class.java)

            val result = apiClient.execute { unreachableService.get() }

            assertTrue(result is ApiResult.NetworkError)
        }
}
