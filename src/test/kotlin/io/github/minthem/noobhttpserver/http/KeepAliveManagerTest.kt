package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.config.KeepAliveConfig
import io.github.minthem.noobhttpserver.io.ByteChannelReadStream
import io.github.minthem.noobhttpserver.io.TimeoutExecutor
import io.github.minthem.noobhttpserver.testutils.FixedReadableByteChannel
import io.github.minthem.noobhttpserver.testutils.InMemoryByteChannel
import io.github.minthem.noobhttpserver.testutils.SideEffectReadableChannel
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
    private val config = KeepAliveConfig(
        enabled = true,
        maxRequests = 100
    )
    private val strategy = KeepAliveManager(timeoutExecutor, config)

    @Nested
    inner class ShouldKeepAliveTest {
        @Test
        fun `KeepAliveが有効な設定だったらtrue`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response = response()
            val context = context()

            val strategy = KeepAliveManager(timeoutExecutor, KeepAliveConfig(enabled = true))
            val actual = strategy.shouldKeepAlive(request, response, context)

            assertTrue(actual)
        }

        @Test
        fun `KeepAliveが無効な設定だったらfalse`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response = response()
            val context = context()

            val strategy = KeepAliveManager(timeoutExecutor, KeepAliveConfig(enabled = false))
            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `回数がしきい値を超えていなかったらtrue`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response = response()
            val context = context(100u)

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertTrue(actual)
        }

        @Test
        fun `回数がしきい値を越えたらfalse`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response = response()
            val context = context(101u)

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `回数がしきい値を越えたらfalse(デフォルト以外)`() {
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
            val request = request(
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.of("connection" to "close")
            )
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `should not keep alive for HTTP 1_1 when request contains close with different case`() {
            val request = request(
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.of("connection" to "Close")
            )
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `should not keep alive when response contains close for HTTP 1_1`() {
            val request = request(HttpProtocol.HTTP_1_1)
            val response = response(
                headers = HttpHeaders.of("connection" to "close")
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
            val request = request(
                protocol = HttpProtocol.HTTP_1_0,
                headers = HttpHeaders.of("connection" to "keep-alive")
            )
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertTrue(actual)
        }

        @Test
        fun `should keep alive for HTTP 1_0 when request contains keep alive with different case`() {
            val request = request(
                protocol = HttpProtocol.HTTP_1_0,
                headers = HttpHeaders.of("connection" to "Keep-Alive")
            )
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertTrue(actual)
        }

        @Test
        fun `should not keep alive for HTTP 1_0 when response contains close`() {
            val request = request(
                protocol = HttpProtocol.HTTP_1_0,
                headers = HttpHeaders.of("connection" to "keep-alive")
            )
            val response = response(
                headers = HttpHeaders.of("connection" to "close")
            )
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertFalse(actual)
        }

        @Test
        fun `should keep alive when HTTP 1_0 connection header contains multiple values including keep alive`() {
            val request = request(
                protocol = HttpProtocol.HTTP_1_0,
                headers = HttpHeaders.of("connection" to "upgrade, keep-alive")
            )
            val response = response()
            val context = context()

            val actual = strategy.shouldKeepAlive(request, response, context)

            assertTrue(actual)
        }

        @Test
        fun `should not keep alive when HTTP 1_1 request connection header contains close among multiple values`() {
            val request = request(
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.of("connection" to "keep-alive, close")
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
        fun `データが来たらReady`() {
            val channel = FixedReadableByteChannel.fromStrings(listOf("G"))
            val stream = ByteChannelReadStream(channel, buffer())

            val actual = strategy.waitForNextRequest(stream)
            assertEquals(actual, WaitResult.Ready)
        }

        @Test
        fun `データが来なかったらEof`() {
            val channel = FixedReadableByteChannel.fromStrings(listOf())
            val stream = ByteChannelReadStream(channel, buffer())

            val actual = strategy.waitForNextRequest(stream)
            assertEquals(actual, WaitResult.Eof)
        }

        @Test
        fun `タイムアウトしたらTimeout`() {
            val channel = SideEffectReadableChannel {
                throw TimeoutException()
            }
            val stream = ByteChannelReadStream(channel, buffer())

            val actual = strategy.waitForNextRequest(stream)
            assertEquals(actual, WaitResult.Timeout)
        }

        @Test
        fun `エラーが起きたらError`() {
            val error = Exception("error")
            val channel = SideEffectReadableChannel {
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
        headers: HttpHeaders = HttpHeaders.EMPTY
    ): HttpRequest {
        return HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/"),
            protocol = protocol,
            headers = headers,
            bodyStream = ByteArrayInputStream(byteArrayOf())
        )
    }

    private fun response(
        headers: HttpHeaders = HttpHeaders.EMPTY
    ): HttpResponse {
        return HttpResponse.build {
            header(headers)
        }
    }

    private fun context(reuseCount: UInt = 0u): ConnectionContext {
        return ConnectionContext(
            "00000000-0000-0000-0000-000000000000",
            reuseCount,
            Clock.System.now(),
            "127.0.0.1",
            60000,
            InMemoryByteChannel.fromStrings(listOf())
        )
    }
}