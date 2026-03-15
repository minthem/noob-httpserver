package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.config.ServerConfig
import io.github.minthem.noobhttpserver.io.TimeoutExecutor
import io.github.minthem.noobhttpserver.router.Router
import io.github.minthem.noobhttpserver.router.RouterRegistry
import io.github.minthem.noobhttpserver.testutils.InMemoryByteChannel
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class ClientSessionHandlerTest {

    private val config = ServerConfig()
    private val timeoutExecutor = TimeoutExecutor(Executors.newSingleThreadScheduledExecutor())
    private val requestParser = HttpRequestParser(
        HttpHeadersParser(config.httpLimits),
        config.httpLimits
    )
    private val responseWriter = HttpResponseWriter(config.buffers.responseHeaderBytes)

    @Test
    fun `handle writes single response and closes session when keep alive is false`() {
        val registry = RouterRegistry().also {
            it.register(
                Router {
                    get("/hello") { _ ->
                        HttpResponse.build {
                            status = HttpStatus.OK
                            header("connection", "close")
                            body("world")
                        }
                    }
                }
            )
        }

        val sessionHandler = ClientSessionHandler(
            handler = RequestHandler(requestParser, RouteResolver(registry)),
            writer = responseWriter,
            keepAliveStrategy = KeepAliveStrategy,
            timeoutExecutor = timeoutExecutor,
            timeoutConfig = config.timeouts,
            requestBufferSize = config.buffers.requestBytes,
        )

        val channel = InMemoryByteChannel.fromStrings(
            listOf(
                "GET /hello HTTP/1.1\r\n",
                "host: localhost\r\n",
                "\r\n"
            )
        )

        sessionHandler.handle(channel)

        val actual = channel.writtenText()
        assertTrue(actual.startsWith("HTTP/1.1 200 OK\r\n"))
        assertContains(actual, "connection: close\r\n")
        assertContains(actual, "\r\n\r\nworld")
    }

    @Test
    fun `handle processes multiple requests when keep alive is true`() {
        val registry = RouterRegistry().also {
            it.register(
                Router {
                    get("/hello") { _ ->
                        HttpResponse.build {
                            status = HttpStatus.OK
                            body("hello")
                        }
                    }
                    get("/bye") { _ ->
                        HttpResponse.build {
                            status = HttpStatus.OK
                            header("connection", "close")
                            body("bye")
                        }
                    }
                }
            )
        }

        val sessionHandler = ClientSessionHandler(
            handler = RequestHandler(requestParser, RouteResolver(registry)),
            writer = responseWriter,
            keepAliveStrategy = KeepAliveStrategy,
            timeoutExecutor = timeoutExecutor,
            timeoutConfig = config.timeouts,
            requestBufferSize = config.buffers.requestBytes,
        )

        val channel = InMemoryByteChannel.fromStrings(
            listOf(
                "GET /hello HTTP/1.1\r\n",
                "host: localhost\r\n",
                "\r\n",
                "GET /bye HTTP/1.1\r\n",
                "host: localhost\r\n",
                "\r\n"
            )
        )

        sessionHandler.handle(channel)

        val actual = channel.writtenText()
        assertContains(actual, "HTTP/1.1 200 OK\r\n")
        assertTrue(actual.contains("\r\n\r\nhello"))
        assertTrue(actual.contains("\r\n\r\nbye"))
    }

    @Test
    fun `handle writes parser error response when request is invalid`() {
        val registry = RouterRegistry()
        val sessionHandler = ClientSessionHandler(
            handler = RequestHandler(requestParser, RouteResolver(registry)),
            writer = responseWriter,
            keepAliveStrategy = KeepAliveStrategy,
            timeoutExecutor = timeoutExecutor,
            timeoutConfig = config.timeouts,
            requestBufferSize = config.buffers.requestBytes,
        )

        val channel = InMemoryByteChannel.fromStrings(
            listOf(
                "INVALID_METHOD_NAME /hello HTTP/1.1\r\n",
                "host: localhost\r\n",
                "\r\n"
            )
        )

        sessionHandler.handle(channel)

        val actual = channel.writtenText()
        assertTrue(actual.startsWith("HTTP/1.1 400 Bad Request\r\n"))
        assertContains(actual, "connection: close\r\n")
    }

    @Test
    fun `handle writes internal server error when unexpected exception occurs`() {
        val registry = RouterRegistry().also {
            it.register(
                Router {
                    get("/explode") { _ ->
                        throw IllegalStateException("boom")
                    }
                }
            )
        }

        val sessionHandler = ClientSessionHandler(
            handler = RequestHandler(requestParser, RouteResolver(registry)),
            writer = responseWriter,
            keepAliveStrategy = KeepAliveStrategy,
            timeoutExecutor = timeoutExecutor,
            timeoutConfig = config.timeouts,
            requestBufferSize = config.buffers.requestBytes,
        )

        val channel = InMemoryByteChannel.fromStrings(
            listOf(
                "GET /explode HTTP/1.1\r\n",
                "host: localhost\r\n",
                "\r\n"
            )
        )

        sessionHandler.handle(channel)

        val actual = channel.writtenText()
        assertTrue(actual.startsWith("HTTP/1.1 500 Internal Server Error\r\n"))
        assertContains(actual, "connection: close\r\n")
    }
}