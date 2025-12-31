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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for the Router class, which handles mapping HTTP requests to specific handlers
 * based on the HTTP method and path patterns.
 */
class RouterTest {

    @Test
    fun `match should return correct handler for a matching GET request`() {
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
        val handler = router.match(request)

        // Assert
        assertNotNull(handler, "A handler should be returned for a matching request.")
    }

    @Test
    fun `match should return null when no route matches`() {
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
        val handler = router.match(request)

        // Assert
        assertNull(handler, "No handler should be returned for a request with an unsupported method.")
    }

    @Test
    fun `match should handle paths with trailing slashes correctly`() {
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
        val handler = router.match(request)

        // Assert
        assertNotNull(handler, "A handler should be returned for a matching request.")
    }

    @Test
    fun `match should distinguish between HTTP methods`() {
        // Arrange
        val router = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
            post("/users") { _ -> HttpResponse.build { status = HttpStatus.CREATED} }
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
        val getHandler = router.match(getRequest)
        val postHandler = router.match(postRequest)

        // Assert
        assertNotNull(getHandler, "A handler should be returned for a matching GET request.")
        assertNotNull(postHandler, "A handler should be returned for a matching POST request.")
    }

    @Test
    fun `match should return correct handler for patterns with static paths`() {
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
        val handler = router.match(request)

        // Assert
        assertNotNull(handler, "A handler should be returned for a matching request.")
    }

    @Test
    fun `match should return null for a request with unmatched static path`() {
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
        val handler = router.match(request)

        // Assert
        assertNull(handler, "No handler should be returned for a request with an unmatched static path.")
    }
}