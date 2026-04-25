package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.config.KeepAliveConfig
import io.github.minthem.noob.http.io.ByteChannelReadStream
import io.github.minthem.noob.http.io.TimeoutExecutor
import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpMethod
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.RequestTarget
import io.github.minthem.noob.http.testutil.FixedReadableByteChannel
import io.github.minthem.noob.http.testutil.InMemoryByteChannel
import io.github.minthem.noob.http.testutil.SideEffectReadableChannel
import org.junit.jupiter.api.Nested
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

class KeepAliveManagerTest {
    private val timeoutExecutor = TimeoutExecutor(Executors.newSingleThreadScheduledExecutor())
    private val config =
        KeepAliveConfig(
            enabled = true,
            maxRequests = 100,
        )
    private val strategy = KeepAliveManager(timeoutExecutor, config)

    @Nested
    inner class ShouldKeepAliveTest {
        @Test
        fun `should return true when keep alive config is enabled`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response = response()
            val context = context()

            val strategy = KeepAliveManager(timeoutExecutor, KeepAliveConfig(enabled = true))
            val actual = strategy.shouldKeepAlive(request, response, context)

            assertTrue(actual)
        }

        @Test
        fun `should return false when keep alive config is disabled`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response = response()
            val context = context()

            val strategy = KeepAliveManager(timeoutExecutor, KeepAliveConfig(enabled = false))
            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `should return true when reuse count is below threshold`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response = response()
            val context = context(100u)

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertTrue(actual)
        }

        @Test
        fun `should return false when reuse count exceeds threshold`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response = response()
            val context = context(101u)

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `should return false when reuse count exceeds custom threshold`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response = response()
            val context = context(1001u)

            val strategy = KeepAliveManager(timeoutExecutor, KeepAliveConfig(enabled = true, maxRequests = 1000))
            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `should keep alive for HTTP 1_1 when request has no connection header and response does not close`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertTrue(actual)
        }

        @Test
        fun `should not keep alive for HTTP 1_1 when request contains close`() {
            val request =
                request(
                    protocol = HttpProtocol.HTTP_1_1,
                    headers = HttpHeaders.of("connection" to "close"),
                )
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `should not keep alive for HTTP 1_1 when request contains close with different case`() {
            val request =
                request(
                    protocol = HttpProtocol.HTTP_1_1,
                    headers = HttpHeaders.of("connection" to "Close"),
                )
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `should not keep alive when response contains close for HTTP 1_1`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response =
                response(
                    headers = HttpHeaders.of("connection" to "close"),
                )
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `should not keep alive for HTTP 1_0 when request has no connection header`() {
            val request = request(HttpProtocol.HTTP_1_0)
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `should keep alive for HTTP 1_0 when request contains keep alive`() {
            val request =
                request(
                    protocol = HttpProtocol.HTTP_1_0,
                    headers = HttpHeaders.of("connection" to "keep-alive"),
                )
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertTrue(actual)
        }

        @Test
        fun `should keep alive for HTTP 1_0 when request contains keep alive with different case`() {
            val request =
                request(
                    protocol = HttpProtocol.HTTP_1_0,
                    headers = HttpHeaders.of("connection" to "Keep-Alive"),
                )
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertTrue(actual)
        }

        @Test
        fun `should not keep alive for HTTP 1_0 when response contains close`() {
            val request =
                request(
                    protocol = HttpProtocol.HTTP_1_0,
                    headers = HttpHeaders.of("connection" to "keep-alive"),
                )
            val response =
                response(
                    headers = HttpHeaders.of("connection" to "close"),
                )
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `should keep alive when HTTP 1_0 connection header contains multiple values including keep alive`() {
            val request =
                request(
                    protocol = HttpProtocol.HTTP_1_0,
                    headers = HttpHeaders.of("connection" to "upgrade, keep-alive"),
                )
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertTrue(actual)
        }

        @Test
        fun `should not keep alive when HTTP 1_1 request connection header contains close among multiple values`() {
            val request =
                request(
                    protocol = HttpProtocol.HTTP_1_1,
                    headers = HttpHeaders.of("connection" to "keep-alive, close"),
                )
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }
    }

    @Nested
    inner class WaitForNextRequestTest {
        @Test
        fun `should return Ready when data arrives`() {
            val channel = FixedReadableByteChannel.fromStrings(listOf("G"))
            val stream = ByteChannelReadStream(channel, buffer())

            val actual = strategy.waitForNextRequest(stream)
            assertEquals(actual, WaitResult.Ready)
        }

        @Test
        fun `should return Eof when no data arrives`() {
            val channel = FixedReadableByteChannel.fromStrings(listOf())
            val stream = ByteChannelReadStream(channel, buffer())

            val actual = strategy.waitForNextRequest(stream)
            assertEquals(actual, WaitResult.Eof)
        }

        @Test
        fun `should return Timeout when timeout occurs`() {
            val channel =
                SideEffectReadableChannel {
                    throw TimeoutException()
                }
            val stream = ByteChannelReadStream(channel, buffer())

            val actual = strategy.waitForNextRequest(stream)
            assertEquals(actual, WaitResult.Timeout)
        }

        @Test
        fun `should return Error when exception occurs`() {
            val error = Exception("error")
            val channel =
                SideEffectReadableChannel {
                    throw error
                }
            val stream = ByteChannelReadStream(channel, buffer())

            val actual = strategy.waitForNextRequest(stream)
            assertTrue(actual is WaitResult.Error)
            assertTrue(actual.cause is Exception)
        }

        private fun buffer() = ByteBuffer.allocate(1024).flip()
    }

    private fun request(
        protocol: HttpProtocol,
        headers: HttpHeaders = HttpHeaders.EMPTY,
    ): HttpRequest =
        HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/"),
            protocol = protocol,
            headers = headers,
            bodyStream = ByteArrayInputStream(byteArrayOf()),
        )

    private fun response(headers: HttpHeaders = HttpHeaders.EMPTY): HttpResponse =
        HttpResponse.build {
            header(headers)
        }

    private fun context(reuseCount: UInt = 0u): ConnectionContext =
        ConnectionContext(
            "00000000-0000-0000-0000-000000000000",
            reuseCount,
            Clock.System.now(),
            "127.0.0.1",
            60000,
            InMemoryByteChannel.fromStrings(listOf()),
        )
}
