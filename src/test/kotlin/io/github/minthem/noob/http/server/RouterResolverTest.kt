package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.exception.MethodNotAllowException
import io.github.minthem.noob.http.exception.RouteNotFoundException
import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpMethod
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.RequestTarget
import io.github.minthem.noob.http.router.Router
import io.github.minthem.noob.http.router.RouterMatchResult
import io.github.minthem.noob.http.router.RouterRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RouteResolverTest {

    @Test
    fun `resolve returns match when registry finds route`() {
        val registry = RouterRegistry().also {
            it.register(
                Router {
                    get("/users/{id}") { _ -> HttpResponse.build {} }
                }
            )
        }
        val resolver = RouteResolver(registry)
        val request = request(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42")
        )

        val actual = resolver.resolve(request)

        assertIs<RouterMatchResult.Match>(actual)
        assertEquals(mapOf("id" to "42"), actual.pathParams)
    }

    @Test
    fun `resolve throws MethodNotAllowException when method does not match`() {
        val registry = RouterRegistry().also {
            it.register(
                Router {
                    get("/users/{id}") { _ -> HttpResponse.build {} }
                }
            )
        }
        val resolver = RouteResolver(registry)
        val request = request(
            method = HttpMethod.POST,
            path = RequestTarget("/users/42")
        )

        val actual = assertFailsWith<MethodNotAllowException> {
            resolver.resolve(request)
        }

        assertEquals(HttpMethod.POST, actual.requestMethod)
        assertEquals(setOf(HttpMethod.GET), actual.allowedMethods)
    }

    @Test
    fun `resolve preserves all allowed methods in MethodNotAllowException`() {
        val registry = RouterRegistry().also {
            it.register(
                Router {
                    get("/users/{id}") { _ -> HttpResponse.build {} }
                    post("/users/{id}") { _ -> HttpResponse.build {} }
                }
            )
        }
        val resolver = RouteResolver(registry)
        val request = request(
            method = HttpMethod.DELETE,
            path = RequestTarget("/users/42")
        )
        val actual = assertFailsWith<MethodNotAllowException> {
            resolver.resolve(request)
        }

        assertEquals(HttpMethod.DELETE, actual.requestMethod)
        assertEquals(setOf(HttpMethod.GET, HttpMethod.POST), actual.allowedMethods)
    }

    @Test
    fun `resolve throws RouteNotFoundException when route is not found`() {
        val registry = RouterRegistry().also {
            it.register(
                Router {
                    get("/users/{id}") { _ -> HttpResponse.build {} }
                }
            )
        }
        val resolver = RouteResolver(registry)
        val request = request(
            method = HttpMethod.GET,
            path = RequestTarget("/orders/42")
        )

        val actual = assertFailsWith<RouteNotFoundException> {
            resolver.resolve(request)
        }

        assertEquals(HttpMethod.GET, actual.method)
        assertEquals(RequestTarget("/orders/42"), actual.requestTarget)
    }


    private fun request(
        method: HttpMethod,
        path: RequestTarget
    ): HttpRequest {
        return HttpRequest(
            method = method,
            path = path,
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
    }
}