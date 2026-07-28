package com.thanhng224.androidxmlbase.core.network.transfer

import app.cash.turbine.test
import com.thanhng224.androidxmlbase.core.architecture.DefaultAppDispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OkHttpFileTransferClientTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpFileTransferClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client =
            OkHttpFileTransferClient(
                okHttpClient = OkHttpClient(),
                dispatchers = DefaultAppDispatchers(),
            )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `download writes body to destination and emits success`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("file-content"))
            val destination = File(temporaryFolder.root, "download.txt")
            val request = Request.Builder().url(server.url("/file")).build()

            client.download(request, destination).test {
                val progress = awaitItem()
                assertTrue(progress is TransferResult.Progress)
                assertEquals(TransferResult.Success(destination), awaitItem())
                awaitComplete()
            }

            assertEquals("file-content", destination.readText())
        }

    @Test
    fun `download emits failure for non-successful response`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(404).setBody("missing"))
            val destination = File(temporaryFolder.root, "missing.txt")
            val request = Request.Builder().url(server.url("/missing")).build()

            client.download(request, destination).test {
                val result = awaitItem()
                assertTrue(result is TransferResult.Failure)
                awaitComplete()
            }
        }

    @Test
    fun `upload emits progress and response body`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("uploaded"))
            val request =
                Request
                    .Builder()
                    .url(server.url("/upload"))
                    .post("payload".toRequestBody("text/plain".toMediaType()))
                    .build()

            client.upload(request).test {
                assertTrue(awaitItem() is TransferResult.Progress)
                assertEquals(
                    TransferResult.Success(HttpTransferResponse(code = 200, body = "uploaded")),
                    awaitItem(),
                )
                awaitComplete()
            }

            assertEquals("payload", server.takeRequest().body.readUtf8())
        }

    @Test
    fun `stream emits chunks in order`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("abcdef"))
            val request = Request.Builder().url(server.url("/stream")).build()

            client.stream(request, chunkSizeBytes = 2).test {
                assertEquals(TransferResult.Success(StreamChunk("ab".toByteArray(), bytesRead = 2)), awaitItem())
                assertEquals(TransferResult.Success(StreamChunk("cd".toByteArray(), bytesRead = 4)), awaitItem())
                assertEquals(TransferResult.Success(StreamChunk("ef".toByteArray(), bytesRead = 6)), awaitItem())
                awaitComplete()
            }
        }
}
