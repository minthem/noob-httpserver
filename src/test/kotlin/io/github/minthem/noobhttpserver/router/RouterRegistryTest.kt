package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpHeaders
import io.github.minthem.noobhttpserver.http.HttpMethod
import io.github.minthem.noobhttpserver.http.HttpProtocol
import io.github.minthem.noobhttpserver.http.HttpRequest
import io.github.minthem.noobhttpserver.http.HttpResponse
import io.github.minthem.noobhttpserver.http.RequestTarget
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the RouterRegistry class, which manages a registry of routers
 * and determines the correct route match for an incoming HTTP request.
 */
class RouterRegistryTest {

    @Test
    fun `find should return RouterMatchResult Match when a router finds a matching route`() {
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
        assertTrue(result is RouterMatchResult.Match, "RouterMatchResult.Match should be returned for a matching route.")
        assertEquals(mapOf("id" to "42"), result.pathParams)
    }

    @Test
    fun `find should return RouterMatchResult NotMatch when no routers have a matching route`() {
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
            result is RouterMatchResult.NotMatch,
            "RouterMatchResult.NotMatch should be returned for an unmatched route."
        )
    }

    @Test
    fun `find should return RouterMatchResult MethodNotMatch when HTTP method is unsupported for a matching path`() {
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
            result is RouterMatchResult.MethodNotMatch,
            "RouterMatchResult.MethodNotMatch should be returned for an unsupported method."
        )
        assertEquals(setOf(HttpMethod.GET), result.allowedMethods)
    }

    @Test
    fun `find should iterate over multiple routers and return the first matching RouterMatchResult Match`() {
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
            result is RouterMatchResult.Match,
            "RouterMatchResult.Match should be returned for the first matching route."
        )
        assertEquals(mapOf("id" to "123"), result.pathParams)
    }

    @Test
    fun `find should return RouterMatchResult NotMatch if all routers return NotMatch`() {
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
            result is RouterMatchResult.NotMatch,
            "RouterMatchResult.NotMatch should be returned when no match is found in any router."
        )
    }

    @Test
    fun `find should return Match when a later router matches after an earlier router returned NotMatch`() {
        val firstRouter = Router {
            get("/orders/{id}") { _ -> HttpResponse.build {} }
        }
        val secondRouter = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val registry = RouterRegistry().also {
            it.register(firstRouter)
            it.register(secondRouter)
        }

        val result = registry.find(request)

        assertTrue(result is RouterMatchResult.Match)
        assertEquals(mapOf("id" to "42"), result.pathParams)
    }

    @Test
    fun `find should return Match when a later router matches after an earlier router returned MethodNotMatch`() {
        val firstRouter = Router {
            post("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val secondRouter = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val registry = RouterRegistry().also {
            it.register(firstRouter)
            it.register(secondRouter)
        }

        val result = registry.find(request)

        assertTrue(result is RouterMatchResult.Match)
        assertEquals(mapOf("id" to "42"), result.pathParams)
    }

    @Test
    fun `find should merge allowed methods from multiple routers when no router matches the request method`() {
        val firstRouter = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val secondRouter = Router {
            post("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.PUT,
            path = RequestTarget("/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val registry = RouterRegistry().also {
            it.register(firstRouter)
            it.register(secondRouter)
        }

        val result = registry.find(request)

        assertTrue(result is RouterMatchResult.MethodNotMatch)
        assertEquals(setOf(HttpMethod.GET, HttpMethod.POST), result.allowedMethods)
    }

    @Test
    fun `find should prefer first registered router match even if later router is more specific`() {
        val firstRouter = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val secondRouter = Router {
            get("/users/me") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/me"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val registry = RouterRegistry().also {
            it.register(firstRouter)
            it.register(secondRouter)
        }

        val result = registry.find(request)

        assertTrue(result is RouterMatchResult.Match)
        assertEquals(
            mapOf("id" to "me"),
            result.pathParams,
            "RouterRegistry should use registration order across routers."
        )
    }

    @Test
    fun `find should stop at first Match and not be affected by later routers`() {
        val firstRouter = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val secondRouter = Router {
            post("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val registry = RouterRegistry().also {
            it.register(firstRouter)
            it.register(secondRouter)
        }

        val result = registry.find(request)

        assertTrue(result is RouterMatchResult.Match)
        assertEquals(mapOf("id" to "42"), result.pathParams)
    }
}