package io.github.minthem.noob.http.router

import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpMethod
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.HttpStatus
import io.github.minthem.noob.http.message.MutableHttpHeaders
import io.github.minthem.noob.http.message.RequestTarget
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
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
        assertTrue(result is RouterMatchResult.Match, "A handler should be returned for a matching request.")
        assertEquals(mapOf("id" to "42"), result.pathParams)
    }

    @Test
    fun `findRoute should return MethodNotMatch when path matches but method is unsupported`() {
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

        val result = router.findRoute(request)

        assertTrue(
            result is RouterMatchResult.MethodNotMatch,
            "MethodNotMatch should be returned when only the HTTP method does not match."
        )
        assertEquals(setOf(HttpMethod.POST), result.allowedMethods)
    }

    @Test
    fun `findRoute should handle paths with trailing slashes correctly`() {
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

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match, "A handler should be returned for a matching request.")
        assertEquals(mapOf("id" to "42"), result.pathParams)
    }

    @Test
    fun `findRoute should distinguish between HTTP methods`() {
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

        val getMatch = router.findRoute(getRequest)
        val postMatch = router.findRoute(postRequest)

        assertTrue(getMatch is RouterMatchResult.Match, "A handler should be returned for a matching GET request.")
        assertTrue(postMatch is RouterMatchResult.Match, "A handler should be returned for a matching POST request.")
    }

    @Test
    fun `findRoute should return correct handler for patterns with static paths`() {
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

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match, "A handler should be returned for a matching request.")
    }

    @Test
    fun `findRoute should return NotMatch for a request with unmatched static path`() {
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

        val result = router.findRoute(request)

        assertTrue(
            result is RouterMatchResult.NotMatch,
            "No handler should be returned for a request with an unmatched static path."
        )
    }

    @Test
    fun `findRoute should return correct handler use group`() {
        val router = Router {
            group("/api/users") {
                get("/{id}") { _ -> HttpResponse.build {} }
                post("") { _ -> HttpResponse.build {} }
            }
        }

        val getRequest = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/api/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )
        val postRequest = HttpRequest(
            method = HttpMethod.POST,
            path = RequestTarget("/api/users"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val getMatch = router.findRoute(getRequest)
        val postMatch = router.findRoute(postRequest)

        assertTrue(getMatch is RouterMatchResult.Match, "A handler should be returned for a matching GET request.")
        assertTrue(postMatch is RouterMatchResult.Match, "A handler should be returned for a matching POST request.")
    }

    @Test
    fun `findRoute should return correct handler use group with trailing slash`() {
        val router = Router {
            group("/api/users/") {
                get("/{id}") { _ -> HttpResponse.build {} }
            }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/api/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match, "A handler should be returned for a matching request.")
    }

    @Test
    fun `findRoute should return correct handler use group with path parameter`() {
        val router = Router {
            group("/api/users/{id}") {
                get("/favorite/{favId}") { _ -> HttpResponse.build {} }
            }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/api/users/42/favorite/123"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match, "A handler should be returned for a matching request.")
        assertEquals(mapOf("id" to "42", "favId" to "123"), result.pathParams)
    }

    @Test
    fun `findRoute should match request path even when query string exists`() {
        val router = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42?tab=profile"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match)
        assertEquals(mapOf("id" to "42"), result.pathParams)
    }

    @Test
    fun `findRoute should treat blank route pattern as root`() {
        val router = Router {
            get("") { _ -> HttpResponse.build {} }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match)
    }

    @Test
    fun `findRoute should match explicit root route`() {
        val router = Router {
            get("/") { _ -> HttpResponse.build {} }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match)
    }

    @Test
    fun `findRoute should prefer later Match over earlier MethodNotMatch`() {
        val router = Router {
            post("/users/{id}") { _ -> HttpResponse.build {} }
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match)
        assertEquals(mapOf("id" to "42"), result.pathParams)
    }

    @Test
    fun `findRoute should prefer more specific static route over dynamic route`() {
        val router = Router {
            get("/users/{id}") { _ ->
                HttpResponse.build {
                    status = HttpStatus.OK
                    header("X-Route", "dynamic")
                }
            }
            get("/users/me") { _ ->
                HttpResponse.build {
                    status = HttpStatus.OK
                    header("X-Route", "static")
                }
            }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/me"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match)
        assertEquals(emptyMap(), result.pathParams, "Static route should be preferred over dynamic route.")
    }

    @Test
    fun `findRoute should prefer more specific nested static route over dynamic route`() {
        val router = Router {
            get("/users/{id}/settings") { _ -> HttpResponse.build {} }
            get("/users/me/settings") { _ -> HttpResponse.build {} }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/me/settings"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match)
        assertEquals(emptyMap(), result.pathParams, "More specific static route should be selected.")
    }

    @Test
    fun `findRoute should prefer route whose earlier segments are static`() {
        val router = Router {
            get("/{resource}/me") { _ -> HttpResponse.build {} }
            get("/users/{id}") { _ -> HttpResponse.build {} }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/me"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match)
        assertEquals(mapOf("id" to "me"), result.pathParams)
    }

    @Test
    fun `findRoute should prefer route with static segment at later position`() {
        val router = Router {
            get("/users/{id}/{tab}") { _ -> HttpResponse.build {} }
            get("/users/{id}/profile") { _ -> HttpResponse.build {} }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/users/42/profile"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.Match)
        assertEquals(mapOf("id" to "42"), result.pathParams, "Route with later static segment should be selected.")
    }

    @Test
    fun `findRoute should return MethodNotMatch when group path matches but nested route method does not`() {
        val router = Router {
            group("/api/users") {
                post("/{id}") { _ -> HttpResponse.build {} }
            }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/api/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.MethodNotMatch)
        assertEquals(setOf(HttpMethod.POST), result.allowedMethods)
    }

    @Test
    fun `findRoute should return NotMatch when group path matches but nested route path does not`() {
        val router = Router {
            group("/api/users") {
                get("/{id}/favorites") { _ -> HttpResponse.build {} }
            }
        }

        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/api/users/42/orders"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.NotMatch)
    }

    @Test
    fun `findRoute should merge allowed methods from routes with same matched path`() {
        val router = Router {
            get("/users/{id}") { _ -> HttpResponse.build {} }
            post("/users/{id}") { _ -> HttpResponse.build {} }
        }

        val request = HttpRequest(
            method = HttpMethod.PUT,
            path = RequestTarget("/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.MethodNotMatch)
        assertEquals(
            setOf(HttpMethod.GET, HttpMethod.POST),
            result.allowedMethods,
            "Allowed methods should be merged for the same matched path."
        )
    }

    @Test
    fun `findRoute should merge allowed methods across group and top level routes when path matches`() {
        val router = Router {
            group("/api/users") {
                get("/{id}") { _ -> HttpResponse.build {} }
            }
            post("/api/users/{id}") { _ -> HttpResponse.build {} }
        }

        val request = HttpRequest(
            method = HttpMethod.DELETE,
            path = RequestTarget("/api/users/42"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = "".byteInputStream()
        )

        val result = router.findRoute(request)

        assertTrue(result is RouterMatchResult.MethodNotMatch)
        assertEquals(
            setOf(HttpMethod.GET, HttpMethod.POST),
            result.allowedMethods,
            "Allowed methods should be merged across group and top level routes."
        )
    }
}