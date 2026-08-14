package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.config.ServerConfig
import io.github.minthem.noob.http.exception.HttpResponseException
import io.github.minthem.noob.http.exception.RouteNotFoundException
import io.github.minthem.noob.http.interceptor.Chain
import io.github.minthem.noob.http.interceptor.Interceptor
import io.github.minthem.noob.http.interceptor.InterceptorRegistry
import io.github.minthem.noob.http.io.ByteChannelReadStream
import io.github.minthem.noob.http.message.HttpMethod
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.HttpStatus
import io.github.minthem.noob.http.message.RequestTarget
import io.github.minthem.noob.http.parser.HttpHeadersParser
import io.github.minthem.noob.http.parser.HttpRequestParser
import io.github.minthem.noob.http.router.Router
import io.github.minthem.noob.http.router.RouterRegistry
import io.github.minthem.noob.http.testutil.FixedReadableByteChannel
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RequestHandlerTest {
    private val config = ServerConfig()
    private val requestParser =
        HttpRequestParser(
            HttpHeadersParser(config.httpLimits),
            config.httpLimits,
        )

    @Test
    fun `process returns request and response when route handler succeeds`() {
        val registry =
            RouterRegistry().also {
                it.register(
                    Router {
                        get("/hello") { _ ->
                            HttpResponse.build {
                                status = HttpStatus.OK
                                body("world")
                            }
                        }
                    },
                )
            }
        val handler =
            RequestHandler(
                parser = requestParser,
                routeResolver = RouteResolver(registry),
                interceptorRegistry = InterceptorRegistry(),
            )
        val stream =
            ByteChannelReadStream(
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /hello HTTP/1.1\r\n",
                        "host: localhost\r\n",
                        "\r\n",
                    ),
                ),
                ByteBuffer.allocate(1024).flip(),
            )

        val actual = handler.process(stream)

        assertEquals(HttpMethod.GET, actual.request.method)
        assertEquals(RequestTarget("/hello"), actual.request.path)
        assertEquals(HttpProtocol.HTTP_1_1, actual.request.protocol)
        assertEquals(HttpStatus.OK, actual.response.status)
    }

    @Test
    fun `process runs interceptors before the route handler`() {
        val events = mutableListOf<String>()
        val registry =
            RouterRegistry().also {
                it.register(
                    Router {
                        get("/users/{id}") { context ->
                            events += "handler:${context.pathParams["id"]}"
                            HttpResponse.build { status = HttpStatus.OK }
                        }
                    },
                )
            }
        val interceptor =
            object : Interceptor {
                override fun intercept(chain: Chain): HttpResponse {
                    events += "interceptor:${chain.context.pathParams["id"]}"
                    return chain.proceed()
                }
            }
        val handler =
            RequestHandler(
                parser = requestParser,
                routeResolver = RouteResolver(registry),
                interceptorRegistry = InterceptorRegistry(listOf(interceptor)),
            )

        val actual = handler.process(requestStream("GET /users/42 HTTP/1.1"))

        assertEquals(HttpStatus.OK, actual.response.status)
        assertEquals(listOf("interceptor:42", "handler:42"), events)
    }

    @Test
    fun `process returns an interceptor response without invoking the route handler`() {
        var routeHandlerCalls = 0
        val registry =
            RouterRegistry().also {
                it.register(
                    Router {
                        get("/protected") {
                            routeHandlerCalls++
                            HttpResponse.build { status = HttpStatus.OK }
                        }
                    },
                )
            }
        val interceptor =
            object : Interceptor {
                override fun intercept(chain: Chain): HttpResponse =
                    HttpResponse.build {
                        status = HttpStatus.FORBIDDEN
                    }
            }
        val handler =
            RequestHandler(
                parser = requestParser,
                routeResolver = RouteResolver(registry),
                interceptorRegistry = InterceptorRegistry(listOf(interceptor)),
            )

        val actual = handler.process(requestStream("GET /protected HTTP/1.1"))

        assertEquals(HttpStatus.FORBIDDEN, actual.response.status)
        assertEquals(0, routeHandlerCalls)
    }

    @Test
    fun `process returns error response when route resolver throws HttpResponseException`() {
        val registry = RouterRegistry()
        val handler =
            RequestHandler(
                parser = requestParser,
                routeResolver = RouteResolver(registry),
                interceptorRegistry = InterceptorRegistry(),
            )
        val stream =
            ByteChannelReadStream(
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /missing HTTP/1.1\r\n",
                        "host: localhost\r\n",
                        "\r\n",
                    ),
                ),
                ByteBuffer.allocate(1024).flip(),
            )

        val exp =
            assertFailsWith<RouteNotFoundException> {
                handler.process(stream)
            }

        assertEquals(HttpMethod.GET, exp.method)
        assertEquals(RequestTarget("/missing"), exp.requestTarget)

        val response = exp.httpResponse
        assertEquals(HttpStatus.NOT_FOUND, response.status)
        assertEquals("close", response.headers["connection"])
    }

    @Test
    fun `process returns error response when route handler throws HttpResponseException`() {
        val registry =
            RouterRegistry().also {
                it.register(
                    Router {
                        get("/fail") { _ ->
                            throw HttpResponseException(
                                message = "boom",
                                httpResponse =
                                    HttpResponse.build {
                                        status = HttpStatus.BAD_REQUEST
                                        header("connection", "close")
                                    },
                            )
                        }
                    },
                )
            }
        val handler =
            RequestHandler(
                parser = requestParser,
                routeResolver = RouteResolver(registry),
                interceptorRegistry = InterceptorRegistry(),
            )
        val stream =
            ByteChannelReadStream(
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /fail HTTP/1.1\r\n",
                        "host: localhost\r\n",
                        "\r\n",
                    ),
                ),
                ByteBuffer.allocate(1024).flip(),
            )

        val actual = handler.process(stream)

        assertEquals(HttpMethod.GET, actual.request.method)
        assertEquals(RequestTarget("/fail"), actual.request.path)
        assertEquals(HttpStatus.BAD_REQUEST, actual.response.status)
        assertEquals("close", actual.response.headers["connection"])
    }

    @Test
    fun `process rethrows unexpected exception from route handler`() {
        val registry =
            RouterRegistry().also {
                it.register(
                    Router {
                        get("/explode") { _ ->
                            throw IllegalStateException("unexpected")
                        }
                    },
                )
            }
        val handler =
            RequestHandler(
                parser = requestParser,
                routeResolver = RouteResolver(registry),
                interceptorRegistry = InterceptorRegistry(),
            )
        val stream =
            ByteChannelReadStream(
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /explode HTTP/1.1\r\n",
                        "host: localhost\r\n",
                        "\r\n",
                    ),
                ),
                ByteBuffer.allocate(1024).flip(),
            )

        val actual =
            assertFailsWith<IllegalStateException> {
                handler.process(stream)
            }

        assertEquals("unexpected", actual.message)
    }

    @Test
    fun `process rethrows parser HttpResponseException when parsing fails`() {
        val registry = RouterRegistry()
        val handler =
            RequestHandler(
                parser = requestParser,
                routeResolver = RouteResolver(registry),
                interceptorRegistry = InterceptorRegistry(),
            )
        val stream =
            ByteChannelReadStream(
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "INVALID_METHOD_NAME /hello HTTP/1.1\r\n",
                        "host: localhost\r\n",
                        "\r\n",
                    ),
                ),
                ByteBuffer.allocate(1024).flip(),
            )

        val actual =
            assertFailsWith<HttpResponseException> {
                handler.process(stream)
            }

        assertEquals(HttpStatus.BAD_REQUEST, actual.httpResponse.status)
        assertEquals("close", actual.httpResponse.headers["connection"])
    }

    private fun requestStream(requestLine: String): ByteChannelReadStream =
        ByteChannelReadStream(
            FixedReadableByteChannel.fromStrings(
                listOf(
                    "$requestLine\r\n",
                    "host: localhost\r\n",
                    "\r\n",
                ),
            ),
            ByteBuffer.allocate(1024).flip(),
        )
}
