package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpHeaders
import io.github.minthem.noobhttpserver.http.HttpMethod
import io.github.minthem.noobhttpserver.http.HttpProtocol
import io.github.minthem.noobhttpserver.http.HttpRequest
import io.github.minthem.noobhttpserver.http.HttpResponse
import io.github.minthem.noobhttpserver.http.HttpStatus
import io.github.minthem.noobhttpserver.http.MutableHttpHeaders
import io.github.minthem.noobhttpserver.http.RequestTarget
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * Unit tests for the Router class, which handles mapping HTTP requests to specific handlers
 * based on the HTTP method and path patterns.
 */
class RouterTest {

    @Test
    fun `findRoute should return correct handler for a matching GET request`() {
        // Arrange
        val router = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = MutableHttpHeaders(),
            bodyStream = "".byteInputStream()
        )

        // Act
        val result = router.findRoute(request)

        // Assert
        assertTrue(result is RouteMatchResult.Match, "A handler should be returned for a matching request.")
    }

    @Test
    fun `findRoute should return null when no route matches`() {
        // Arrange
        val router = Router {
            post("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = MutableHttpHeaders(),
            bodyStream = "".byteInputStream()
        )

        // Act
        val result = router.findRoute(request)

        // Assert
        assertTrue(
            result is RouteMatchResult.MethodNotAllowed,
            "No handler should be returned for a request with an unsupported method."
        )
    }

    @Test
    fun `findRoute should handle paths with trailing slashes correctly`() {
        // Arrange
        val router = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42/"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        // Act
        val result = router.findRoute(request)

        // Assert
        assertTrue(result is RouteMatchResult.Match, "A handler should be returned for a matching request.")
    }

    @Test
    fun `findRoute should distinguish between HTTP methods`() {
        // Arrange
        val router = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
            post("/users") { _ -> HttpResponse.build { status = HttpStatus.CREATED } }
        }
        val getRequest = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val postRequest = HttpRequest(
            method = HttpMethod.POST,
            path = RequestTarget("/users"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        // Act
        val getMatch = router.findRoute(getRequest)
        val postMatch = router.findRoute(postRequest)

        // Assert
        assertTrue(getMatch is RouteMatchResult.Match, "A handler should be returned for a matching GET request.")
        assertTrue(postMatch is RouteMatchResult.Match, "A handler should be returned for a matching POST request.")
    }

    @Test
    fun `findRoute should return correct handler for patterns with static paths`() {
        // Arrange
        val router = Router {
            get("/static/path") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/static/path"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        // Act
        val result = router.findRoute(request)

        // Assert
        assertTrue(result is RouteMatchResult.Match, "A handler should be returned for a matching request.")
    }

    @Test
    fun `findRoute should return null for a request with unmatched static path`() {
        // Arrange
        val router = Router {
            get("/static/path") { _ -> HttpResponse.build {} }
        }
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/different/path"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        // Act
        val result = router.findRoute(request)

        // Assert
        assertTrue(
            result is RouteMatchResult.NotFound,
            "No handler should be returned for a request with an unmatched static path."
        )
    }
}