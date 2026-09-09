package com.thanhng224.androidcorebase.core.network.transfer

import app.cash.turbine.test
import com.thanhng224.androidcorebase.core.architecture.DefaultAppDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

private fun <P, R> TransferEvent<P, R>.requirePayload(): P {
    check(this is TransferEvent.Payload<P>) { "Expected Payload but was $this" }
    return value
}

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
    fun `download writes body to destination and emits progress then Completed`() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200).setBody("file-content"))
            val destination = File(temporaryFolder.root, "download.txt")
            val request = Request.Builder().url(server.url("/file")).build()
            val events = mutableListOf<DownloadEvent>()

            client.download(request, destination).collect { events.add(it) }

            assertEquals(1, events.count { it is TransferEvent.Completed })
            assertEquals(0, events.count { it is TransferEvent.Failed })
            assertEquals(TransferEvent.Completed(destination), events.last())
            assertEquals("file-content", destination.readText())
        }

    @Test
    fun `download emits Http failure with the response code on a non-successful response`() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(404).setBody("missing"))
            val destination = File(temporaryFolder.root, "missing.txt")
            val request = Request.Builder().url(server.url("/missing")).build()

            client.download(request, destination).test {
                val event = awaitItem()
                assertEquals(TransferEvent.Failed(TransferError.Http(404)), event)
                awaitComplete()
            }
        }

    @Test
    fun `download emits EmptyBody failure for a 204 No Content response`() =
        runBlocking {
            // 204/205 must not carry a body per HTTP spec, regardless of what the transport
            // reports for contentLength(); unlike Retrofit's body(), raw OkHttp's Response.body
            // is never null (it defaults to ResponseBody.EMPTY), so the status code -- not a
            // null check -- is the only reliable signal here.
            server.enqueue(MockResponse().setResponseCode(204))
            val destination = File(temporaryFolder.root, "empty.txt")
            val request = Request.Builder().url(server.url("/file")).build()

            client.download(request, destination).test {
                val event = awaitItem()
                assertEquals(TransferEvent.Failed(TransferError.EmptyBody), event)
                awaitComplete()
            }
        }

    @Test
    fun `download emits FileSystem failure when destination cannot be opened for writing`() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200).setBody("data"))
            val blocker = File(temporaryFolder.root, "blocker").apply { writeText("x") }
            val destination = File(blocker, "nested/destination.txt")
            val request = Request.Builder().url(server.url("/file")).build()

            client.download(request, destination).test {
                val event = awaitItem()
                assertTrue(event is TransferEvent.Failed)
                assertTrue((event as TransferEvent.Failed).error is TransferError.FileSystem)
                awaitComplete()
            }
        }

    @Test
    fun `download preserves the existing destination when the transfer disconnects mid-stream`() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("first-chunk-then-drop-" + "x".repeat(64 * 1024))
                    .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
            )
            val destination = File(temporaryFolder.root, "existing.txt").apply { writeText("old-content") }
            val request = Request.Builder().url(server.url("/disconnect")).build()
            val events = mutableListOf<DownloadEvent>()

            client.download(request, destination).collect { events.add(it) }

            assertEquals(1, events.count { it is TransferEvent.Failed })
            assertEquals(0, events.count { it is TransferEvent.Completed })
            assertTrue((events.last() as TransferEvent.Failed).error is TransferError.Network)
            assertEquals("old-content", destination.readText())
        }

    @Test
    fun `download starts an independent HTTP call on every collection`() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200).setBody("one"))
            server.enqueue(MockResponse().setResponseCode(200).setBody("two"))
            val request = Request.Builder().url(server.url("/file")).build()

            client.download(request, File(temporaryFolder.root, "first.txt")).collect { }
            client.download(request, File(temporaryFolder.root, "second.txt")).collect { }

            assertEquals(2, server.requestCount)
        }

    @Test
    fun `cancellation during download propagates without emitting Failed`() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody("x".repeat(200_000))
                    .throttleBody(100, 10, TimeUnit.MILLISECONDS),
            )
            val destination = File(temporaryFolder.root, "cancelled.txt")
            val request = Request.Builder().url(server.url("/slow")).build()
            val collected = mutableListOf<DownloadEvent>()
            var cancellationCaught = false

            val job =
                launch {
                    try {
                        client.download(request, destination).collect { collected.add(it) }
                    } catch (e: CancellationException) {
                        cancellationCaught = true
                        throw e
                    }
                }

            delay(100)
            job.cancelAndJoin()

            assertTrue(cancellationCaught)
            assertTrue(collected.none { it is TransferEvent.Failed })
        }

    @Test
    fun `upload emits progress and Completed with status metadata`() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200).setBody("uploaded"))
            val request =
                Request
                    .Builder()
                    .url(server.url("/upload"))
                    .post("payload".toRequestBody("text/plain".toMediaType()))
                    .build()
            val events = mutableListOf<UploadEvent>()

            client.upload(request).collect { events.add(it) }

            assertEquals(1, events.count { it is TransferEvent.Completed })
            val completed = events.last() as TransferEvent.Completed
            assertEquals(200, completed.value.code)
            assertEquals("payload", server.takeRequest().body.readUtf8())
        }

    @Test
    fun `upload emits Http failure with the response code on a non-successful response`() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(500).setBody("nope"))
            val request =
                Request
                    .Builder()
                    .url(server.url("/upload"))
                    .post("payload".toRequestBody("text/plain".toMediaType()))
                    .build()

            val events = mutableListOf<UploadEvent>()

            client.upload(request).collect { events.add(it) }

            assertEquals(1, events.count { it is TransferEvent.Failed })
            assertEquals(0, events.count { it is TransferEvent.Completed })
            assertEquals(TransferEvent.Failed(TransferError.Http(500)), events.last())
        }

    @Test
    fun `stream emits payload chunks in order then Completed`() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(200).setBody("abcdef"))
            val request = Request.Builder().url(server.url("/stream")).build()

            client.stream(request, chunkSizeBytes = 2).test {
                assertTrue(awaitItem().requirePayload().contentEquals("ab".toByteArray()))
                assertTrue(awaitItem().requirePayload().contentEquals("cd".toByteArray()))
                assertTrue(awaitItem().requirePayload().contentEquals("ef".toByteArray()))
                assertEquals(TransferEvent.Completed(Unit), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `stream emits EmptyBody failure for a 204 No Content response`() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(204))
            val request = Request.Builder().url(server.url("/stream")).build()

            client.stream(request).test {
                assertEquals(TransferEvent.Failed(TransferError.EmptyBody), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `stream fails synchronously for a non-positive chunk size before any Flow is collected`() {
        val request = Request.Builder().url(server.url("/stream")).build()

        try {
            client.stream(request, chunkSizeBytes = 0)
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}
