package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpHeaders
import io.github.minthem.noobhttpserver.http.HttpMethod
import io.github.minthem.noobhttpserver.http.HttpProtocol
import io.github.minthem.noobhttpserver.http.HttpRequest
import io.github.minthem.noobhttpserver.http.HttpResponse
import io.github.minthem.noobhttpserver.http.RequestTarget
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Unit tests for the RouterRegistry class, which manages a registry of routers
 * and determines the correct route match for an incoming HTTP request.
 */
class RouterRegistryTest {

    @Test
    fun `find should return RouteMatchResult Match when a router finds a matching route`() {
        // Arrange
        val router = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val registry = RouterRegistry().also { it.register(router) }

        // Act
        val result = registry.find(request)

        // Assert
        assertTrue(result is RouteMatchResult.Match, "RouteMatchResult.Match should be returned for a matching route.")
    }

    @Test
    fun `find should return RouteMatchResult NotFound when no routers have a matching route`() {
        // Arrange
        val router = Router {
            get("/products") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/nonexistent"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val registry = RouterRegistry().also { it.register(router) }

        // Act
        val result = registry.find(request)

        // Assert
        assertTrue(
            result is RouteMatchResult.NotMatch,
            "RouteMatchResult.NotFound should be returned for an unmatched route."
        )
    }

    @Test
    fun `find should return RouteMatchResult MethodNotAllowed when HTTP method is unsupported for a matching path`() {
        // Arrange
        val router = Router {
            get("/users") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.POST,
            path = RequestTarget("/users"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val registry = RouterRegistry().also { it.register(router) }

        // Act
        val result = registry.find(request)

        // Assert
        assertTrue(
            result is RouteMatchResult.MethodNotMatch,
            "RouteMatchResult.MethodNotAllowed should be returned for an unsupported method."
        )
    }

    @Test
    fun `find should iterate over multiple routers and return the first matching RouteMatchResult Match`() {
        // Arrange
        val firstRouter = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val secondRouter = Router {
            get("/products/{id}") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/123"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val registry = RouterRegistry().also {
            it.register(firstRouter)
            it.register(secondRouter)
        }

        // Act
        val result = registry.find(request)

        // Assert
        assertTrue(
            result is RouteMatchResult.Match,
            "RouteMatchResult.Match should be returned for the first matching route."
        )
    }

    @Test
    fun `find should return RouteMatchResult NotFound if all routers return NotFound`() {
        // Arrange
        val firstRouter = Router {
            get("/nonexistent") { _ -> HttpResponse.build {} }
        }
        val secondRouter = Router {
            get("/stillnotfound") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/doesnotexist"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val registry = RouterRegistry().also {
            it.register(firstRouter)
            it.register(secondRouter)
        }

        // Act
        val result = registry.find(request)

        // Assert
        assertTrue(
            result is RouteMatchResult.NotMatch,
            "RouteMatchResult.NotFound should be returned when no match is found in any router."
        )
    }
}