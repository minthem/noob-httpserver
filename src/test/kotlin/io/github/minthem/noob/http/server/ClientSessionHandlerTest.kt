package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.config.HttpLimitsConfig
import io.github.minthem.noob.http.config.KeepAliveConfig
import io.github.minthem.noob.http.config.ServerConfig
import io.github.minthem.noob.http.interceptor.InterceptorRegistry
import io.github.minthem.noob.http.io.TimeoutExecutor
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.HttpStatus
import io.github.minthem.noob.http.parser.HttpHeadersParser
import io.github.minthem.noob.http.parser.HttpRequestParser
import io.github.minthem.noob.http.router.Router
import io.github.minthem.noob.http.router.RouterRegistry
import io.github.minthem.noob.http.testutil.InMemoryByteChannel
import org.junit.jupiter.api.Assertions
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue
import kotlin.time.Clock

class ClientSessionHandlerTest {
    private val config = ServerConfig()
    private val timeoutExecutor = TimeoutExecutor(Executors.newSingleThreadScheduledExecutor())
    private val requestParser =
        HttpRequestParser(
            HttpHeadersParser(config.httpLimits),
            config.httpLimits,
        )
    private val responseWriter = HttpResponseWriter(config.buffers.responseHeaderBytes)

    @Test
    fun `handle writes single response and closes session when keep alive is false`() {
        val registry =
            RouterRegistry().also {
                it.register(
                    Router {
                        get("/hello") { _ ->
                            HttpResponse.build {
                                status = HttpStatus.OK
                                header("connection", "close")
                                body("world")
                            }
                        }
                    },
                )
            }

        val sessionHandler =
            ClientSessionHandler(
                handler = RequestHandler(requestParser, RouteResolver(registry), InterceptorRegistry()),
                writer = responseWriter,
                keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
                timeoutExecutor = timeoutExecutor,
                timeoutConfig = config.timeouts,
                requestBufferSize = config.buffers.requestBytes,
            )

        val channel =
            InMemoryByteChannel.fromStrings(
                listOf(
                    "GET /hello HTTP/1.1\r\n",
                    "host: localhost\r\n",
                    "\r\n",
                ),
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
        val registry =
            RouterRegistry().also {
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
                    },
                )
            }

        val sessionHandler =
            ClientSessionHandler(
                handler = RequestHandler(requestParser, RouteResolver(registry), InterceptorRegistry()),
                writer = responseWriter,
                keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
                timeoutExecutor = timeoutExecutor,
                timeoutConfig = config.timeouts,
                requestBufferSize = config.buffers.requestBytes,
            )

        val channel =
            InMemoryByteChannel.fromStrings(
                listOf(
                    "GET /hello HTTP/1.1\r\n",
                    "host: localhost\r\n",
                    "\r\n",
                    "GET /bye HTTP/1.1\r\n",
                    "host: localhost\r\n",
                    "\r\n",
                ),
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
        val registry =
            RouterRegistry().also {
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
                    },
                )
            }

        val sessionHandler =
            ClientSessionHandler(
                handler = RequestHandler(requestParser, RouteResolver(registry), InterceptorRegistry()),
                writer = responseWriter,
                keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
                timeoutExecutor = timeoutExecutor,
                timeoutConfig = config.timeouts,
                requestBufferSize = config.buffers.requestBytes,
            )

        val channel =
            InMemoryByteChannel.fromStrings(
                listOf(
                    "GET /hello HTTP/1.1\r\n",
                    "host: localhost\r\n",
                    "\r\n",
                    "GET /bye HTTP/1.1\r\n",
                    "host: localhost\r\n",
                    "\r\n",
                ),
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
        val registry =
            RouterRegistry().also {
                it.register(
                    Router {
                        post("/counter") { request ->
                            val count = request.bodyAsText().toInt()
                            HttpResponse.build {
                                body("Counter: $count ====")
                            }
                        }
                    },
                )
            }
        val keepAliveConfig = KeepAliveConfig(maxRequests = 10)

        val sessionHandler =
            ClientSessionHandler(
                handler = RequestHandler(requestParser, RouteResolver(registry), InterceptorRegistry()),
                writer = responseWriter,
                keepAliveManager = KeepAliveManager(timeoutExecutor, keepAliveConfig),
                timeoutExecutor = timeoutExecutor,
                timeoutConfig = config.timeouts,
                requestBufferSize = config.buffers.requestBytes,
            )

        val requestBytes =
            IntRange(0, 20).map {
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
        Assertions.assertFalse(actual.contains("\r\n\r\nCounter: 12 ===="))
    }

    @Test
    fun `handle writes parser error response when request is invalid`() {
        val registry = RouterRegistry()
        val sessionHandler =
            ClientSessionHandler(
                handler = RequestHandler(requestParser, RouteResolver(registry), InterceptorRegistry()),
                writer = responseWriter,
                keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
                timeoutExecutor = timeoutExecutor,
                timeoutConfig = config.timeouts,
                requestBufferSize = config.buffers.requestBytes,
            )

        val channel =
            InMemoryByteChannel.fromStrings(
                listOf(
                    "INVALID_METHOD_NAME /hello HTTP/1.1\r\n",
                    "host: localhost\r\n",
                    "\r\n",
                ),
            )

        val context = createContext(channel)
        sessionHandler.handle(context)

        val actual = channel.writtenText()
        assertTrue(actual.startsWith("HTTP/1.1 400 Bad Request\r\n"))
        assertContains(actual, "connection: close\r\n")
    }

    @Test
    fun `handle writes internal server error when unexpected exception occurs`() {
        val registry =
            RouterRegistry().also {
                it.register(
                    Router {
                        get("/explode") { _ ->
                            throw IllegalStateException("boom")
                        }
                    },
                )
            }

        val sessionHandler =
            ClientSessionHandler(
                handler = RequestHandler(requestParser, RouteResolver(registry), InterceptorRegistry()),
                writer = responseWriter,
                keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
                timeoutExecutor = timeoutExecutor,
                timeoutConfig = config.timeouts,
                requestBufferSize = config.buffers.requestBytes,
            )

        val channel =
            InMemoryByteChannel.fromStrings(
                listOf(
                    "GET /explode HTTP/1.1\r\n",
                    "host: localhost\r\n",
                    "\r\n",
                ),
            )

        val context = createContext(channel)
        sessionHandler.handle(context)

        val actual = channel.writtenText()
        assertTrue(actual.startsWith("HTTP/1.1 500 Internal Server Error\r\n"))
        assertContains(actual, "connection: close\r\n")
    }

    @Test
    fun `handle writes payload too large response when request body exceeds limit`() {
        val registry =
            RouterRegistry().also {
                it.register(
                    Router {
                        post("/large-body") { ctx ->
                            HttpResponse.build {
                                status = HttpStatus.OK
                                body(ctx.bodyAsBytes())
                            }
                        }
                    },
                )
            }

        val limitConfig = HttpLimitsConfig(
            maxRequestBodyBytes = 1025,
        )
        val requestParser = HttpRequestParser(
            headerParser = HttpHeadersParser(limitConfig),
            config = limitConfig,
        )

        val sessionHandler =
            ClientSessionHandler(
                handler = RequestHandler(requestParser, RouteResolver(registry), InterceptorRegistry()),
                writer = responseWriter,
                keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
                timeoutExecutor = timeoutExecutor,
                timeoutConfig = config.timeouts,
                requestBufferSize = config.buffers.requestBytes,
            )

        val channel =
            InMemoryByteChannel.fromStrings(
                listOf(
                    "POST /large-body HTTP/1.1\r\n",
                    "host: localhost\r\n",
                    "transfer-encoding: chunked\r\n",
                    "\r\n",
                    "100\r\n",
                    "${"a".repeat(256)}\r\n",
                    "100\r\n",
                    "${"b".repeat(256)}\r\n",
                    "100\r\n",
                    "${"c".repeat(256)}\r\n",
                    "100\r\n",
                    "${"d".repeat(256)}\r\n",
                    "7\r\n",
                    "is over\r\n",
                    "0\r\n",
                    "\r\n",
                ),
            )

        val context = createContext(channel)
        sessionHandler.handle(context)

        val actual = channel.writtenText()

        assertTrue(actual.startsWith("HTTP/1.1 413 Payload Too Large\r\n"))
        assertContains(actual, "connection: close\r\n")
    }


    @Test
    fun `handle writes payload too large response when chunk size exceeds limit`() {
        val registry =
            RouterRegistry().also {
                it.register(
                    Router {
                        post("/large-body") { ctx ->
                            HttpResponse.build {
                                status = HttpStatus.OK
                                body(ctx.bodyAsBytes())
                            }
                        }
                    },
                )
            }

        val limitConfig = HttpLimitsConfig(
            maxChunkSizeBytes = 1024,
        )
        val requestParser = HttpRequestParser(
            headerParser = HttpHeadersParser(limitConfig),
            config = limitConfig,
        )

        val sessionHandler =
            ClientSessionHandler(
                handler = RequestHandler(requestParser, RouteResolver(registry), InterceptorRegistry()),
                writer = responseWriter,
                keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
                timeoutExecutor = timeoutExecutor,
                timeoutConfig = config.timeouts,
                requestBufferSize = config.buffers.requestBytes,
            )

        val channel =
            InMemoryByteChannel.fromStrings(
                listOf(
                    "POST /large-body HTTP/1.1\r\n",
                    "host: localhost\r\n",
                    "transfer-encoding: chunked\r\n",
                    "\r\n",
                    "401\r\n",
                    "${"a".repeat(1025)}\r\n",
                    "8\r\n",
                    "non read\r\n",
                    "0\r\n",
                    "\r\n",
                ),
            )

        val context = createContext(channel)
        sessionHandler.handle(context)

        val actual = channel.writtenText()

        assertTrue(actual.startsWith("HTTP/1.1 413 Payload Too Large\r\n"))
        assertContains(actual, "connection: close\r\n")
    }

    @Test
    fun `handle does not block infinitely when body size exceeds limit during chunked request`() {
        val registry =
            RouterRegistry().also {
                it.register(
                    Router {
                        post("/large-body") { ctx ->
                            HttpResponse.build {
                                status = HttpStatus.OK
                                body(ctx.bodyAsBytes())
                            }
                        }
                    },
                )
            }

        val limitConfig = HttpLimitsConfig(
            maxRequestBodyBytes = 1025,
        )
        val requestParser = HttpRequestParser(
            headerParser = HttpHeadersParser(limitConfig),
            config = limitConfig,
        )

        val sessionHandler =
            ClientSessionHandler(
                handler = RequestHandler(requestParser, RouteResolver(registry), InterceptorRegistry()),
                writer = responseWriter,
                keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive),
                timeoutExecutor = timeoutExecutor,
                timeoutConfig = config.timeouts.copy(readMillis = 1000, writeMillis = 1000, sessionMillis = 1000), // Ensure quick failure if it blocks
                requestBufferSize = config.buffers.requestBytes,
            )

        // Simulate a request where the claimed chunk size is large, but body limit is exceeded quickly
        val channel =
            InMemoryByteChannel.fromStrings(
                listOf(
                    "POST /large-body HTTP/1.1\r\n",
                    "host: localhost\r\n",
                    "transfer-encoding: chunked\r\n",
                    "\r\n",
                    "1000\r\n", // Pretend it's a huge chunk (4096 bytes)
                    "${"a".repeat(1026)}\r\n", // Actually send just enough to exceed body limit (1025)
                    "0\r\n",
                    "\r\n",
                ),
            )

        val context = createContext(channel)

        // If the bug exists, this call will block infinitely (or hit the 1000ms timeout we set above and fail the keep-alive silently)
        // But we want to ensure it fails FAST due to exhaustion, not due to timeout.
        sessionHandler.handle(context)

        val actual = channel.writtenText()

        assertTrue(actual.startsWith("HTTP/1.1 413 Payload Too Large\r\n"))
        assertContains(actual, "connection: close\r\n")
    }

    private fun createContext(channel: InMemoryByteChannel): ConnectionContext =
        ConnectionContext(
            id = "00000000-0000-0000-0000-000000000000",
            _reuseCount = 0U,
            createdAt = Clock.System.now(),
            remoteIp = "127.0.0.1",
            remotePort = 60000,
            channel = channel,
        )
}
