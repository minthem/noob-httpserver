package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.config.KeepAliveConfig
import io.github.minthem.noobhttpserver.config.ServerConfig
import io.github.minthem.noobhttpserver.io.TimeoutExecutor
import io.github.minthem.noobhttpserver.router.Router
import io.github.minthem.noobhttpserver.router.RouterRegistry
import io.github.minthem.noobhttpserver.testutils.InMemoryByteChannel
import org.junit.jupiter.api.Assertions.assertFalse
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue
import kotlin.time.Clock

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
            keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
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
        val context = createContext(channel)
        sessionHandler.handle(context)

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
            keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
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

        val context = createContext(channel)
        sessionHandler.handle(context)

        val actual = channel.writtenText()
        assertContains(actual, "HTTP/1.1 200 OK\r\n")
        assertTrue(actual.contains("\r\n\r\nhello"))
        assertTrue(actual.contains("\r\n\r\nbye"))
    }

    @Test
    fun `handle processes request but stops when no subsequent data arrives despite keep alive`() {
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
                            body("bye")
                        }
                    }
                }
            )
        }

        val sessionHandler = ClientSessionHandler(
            handler = RequestHandler(requestParser, RouteResolver(registry)),
            writer = responseWriter,
            keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
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

        val context = createContext(channel)
        sessionHandler.handle(context)

        val actual = channel.writtenText()
        assertContains(actual, "HTTP/1.1 200 OK\r\n")
        assertTrue(actual.contains("\r\n\r\nhello"))
        assertTrue(actual.contains("\r\n\r\nbye"))
    }

    @Test
    fun `handle closes session when keep alive limit is reached`() {
        val registry = RouterRegistry().also {
            it.register(
                Router {
                    post("/counter") { request ->
                        val count = request.bodyAsText().toInt()
                        HttpResponse.build {
                            body("Counter: $count ====")
                        }
                    }
                }
            )
        }
        val keepAliveConfig = KeepAliveConfig(maxRequests = 10)

        val sessionHandler = ClientSessionHandler(
            handler = RequestHandler(requestParser, RouteResolver(registry)),
            writer = responseWriter,
            keepAliveManager = KeepAliveManager(timeoutExecutor, keepAliveConfig),
            timeoutExecutor = timeoutExecutor,
            timeoutConfig = config.timeouts,
            requestBufferSize = config.buffers.requestBytes,
        )

        val requestBytes = IntRange(0, 20).map {
            val body = it.toString()
            val length = body.toByteArray().size
            "POST /counter HTTP/1.1\r\nhost: localhost\r\ncontent-length: $length\r\ncontent-type: text/plain\r\n\r\n$body"
        }

        val channel = InMemoryByteChannel.fromStrings(requestBytes)

        val context = createContext(channel)
        sessionHandler.handle(context)

        val actual = channel.writtenText()
        assertContains(actual, "HTTP/1.1 200 OK\r\n")
        assertTrue(actual.contains("\r\n\r\nCounter: 11 ===="))
        assertFalse(actual.contains("\r\n\r\nCounter: 12 ===="))
    }

    @Test
    fun `handle writes parser error response when request is invalid`() {
        val registry = RouterRegistry()
        val sessionHandler = ClientSessionHandler(
            handler = RequestHandler(requestParser, RouteResolver(registry)),
            writer = responseWriter,
            keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
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

        val context = createContext(channel)
        sessionHandler.handle(context)

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
            keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
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

        val context = createContext(channel)
        sessionHandler.handle(context)

        val actual = channel.writtenText()
        assertTrue(actual.startsWith("HTTP/1.1 500 Internal Server Error\r\n"))
        assertContains(actual, "connection: close\r\n")
    }


    private fun createContext(channel: InMemoryByteChannel): ConnectionContext {
        return ConnectionContext(
            id = "00000000-0000-0000-0000-000000000000",
            _reuseCount = 0U,
            createdAt = Clock.System.now(),
            remoteIp = "127.0.0.1",
            remotePort = 60000,
            channel = channel
        )
    }
}