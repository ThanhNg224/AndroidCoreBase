package com.thanhng224.androidcorebase.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
    fun `401 maps to Failure with Http ApiFailure carrying code and message`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setStatus("HTTP/1.1 401 Unauthorized")
                    .setBody("nope"),
            )

            val result = apiClient.execute { service.get() }

            assertEquals(ApiResult.Failure(ApiFailure.Http(401, "Unauthorized")), result)
        }

    @Test
    fun `204 No Content maps to Failure with EmptyBody`() =
        runTest {
            // Retrofit special-cases 204/205 and returns a null body without invoking the
            // converter, which is the only way to reliably get a null response.body() out
            // of the JSON converter (an empty string is not valid JSON and would instead
            // surface as a Serialization failure).
            server.enqueue(MockResponse().setResponseCode(204))

            val result = apiClient.execute { service.get() }

            assertEquals(ApiResult.Failure(ApiFailure.EmptyBody), result)
        }

    @Test
    fun `malformed JSON body maps to Failure with Serialization ApiFailure`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{"not-value": true"""))

            val result = apiClient.execute { service.get() }

            assertTrue(result is ApiResult.Failure)
            assertTrue((result as ApiResult.Failure).error is ApiFailure.Serialization)
        }

    @Test
    fun `unreachable server maps to Failure with Network ApiFailure`() =
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

            assertTrue(result is ApiResult.Failure)
            assertTrue((result as ApiResult.Failure).error is ApiFailure.Network)
        }

    @Test
    fun `cancellation propagates instead of becoming a Failure`() =
        runTest {
            val cancellation = CancellationException("cancelled")

            val thrown =
                try {
                    apiClient.execute<TestDto> { throw cancellation }
                    null
                } catch (e: CancellationException) {
                    e
                }

            assertSame(cancellation, thrown)
        }
}
