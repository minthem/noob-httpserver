package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.RequestTarget
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PathPatternTest {

    @Test
    fun `testMatch should return true when nothing path parameters`() {
        val pattern = PathPattern.parse("/users")
        assertTrue(pattern.match(RequestTarget("/users")) is PathPatternMatchResult.Match)
    }

    @Test
    fun `testMatch should return true when path matches pattern`() {
        val pattern = PathPattern.parse("/users/{id}")

        when (val result = pattern.match(RequestTarget("/users/42"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/42" }
            }
        }

        when (val result = pattern.match(RequestTarget("/users/42/"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/42" }
            }
        }

        when (val result = pattern.match(RequestTarget("/users/test-user"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "test-user"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/test-user" }
            }
        }
    }

    @Test
    fun `testMatch should return true when path parameters in middle`() {
        val pattern = PathPattern.parse("/users/{id}/orders")
        assertTrue(pattern.match(RequestTarget("/users/foo42/orders")) is PathPatternMatchResult.Match)
    }

    @Test
    fun `testMatch should return true when path matches pattern multiple times`() {
        val pattern = PathPattern.parse("/users/{id}/orders/{orderId}")

        when (val result = pattern.match(RequestTarget("/users/42/orders/1234567890"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42", "orderId" to "1234567890"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/42/orders/1234567890" }
            }
        }

        when (val result = pattern.match(RequestTarget("/users/42/orders/1234567890/"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42", "orderId" to "1234567890"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/42/orders/1234567890/" }
            }
        }
    }

    @Test
    fun `testMatch should return true when remains path`() {
        val pattern = PathPattern.parse("/users/{id}/orders/{orderId}", isPrefix = true)

        when (val result = pattern.match(RequestTarget("/users/42/orders/1234567890/items"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42", "orderId" to "1234567890"), result.pathParams)
                assertEquals("/items", result.remainingPath)
            }

            else -> {
                fail { "Expected match for /users/{id}/orders/{orderId}/items" }
            }
        }
    }

    @Test
    fun `testNoMatch should return false when path does not match pattern`() {
        val pattern = PathPattern.parse("/users/{id}")
        assertTrue(pattern.match(RequestTarget("/users/")) is PathPatternMatchResult.NoMatch)
        assertTrue(pattern.match(RequestTarget("/users")) is PathPatternMatchResult.NoMatch)
        assertTrue(pattern.match(RequestTarget("/orders/42")) is PathPatternMatchResult.NoMatch)
    }

    @Test
    fun `testMatch should decode path parameters`() {
        val pattern = PathPattern.parse("/users/{id}")

        when (val result = pattern.match(RequestTarget("/users/%E3%81%82"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "あ"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/%E3%81%82" }
            }
        }
    }

    @Test
    fun `testMatch should ignore query string`() {
        val pattern = PathPattern.parse("/users/{id}")

        when (val result = pattern.match(RequestTarget("/users/42?tab=profile"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/42?tab=profile" }
            }
        }
    }

    @Test
    fun `testMatch should return true for root path`() {
        val pattern = PathPattern.parse("/")

        assertTrue(pattern.match(RequestTarget("/")) is PathPatternMatchResult.Match)
    }

    @Test
    fun `testMatch should return false for non root path when pattern is root`() {
        val pattern = PathPattern.parse("/")

        assertTrue(pattern.match(RequestTarget("/users")) is PathPatternMatchResult.NoMatch)
    }

    @Test
    fun `testPrefixMatch should return slash as remaining path when matched exactly`() {
        val pattern = PathPattern.parse("/users/{id}", isPrefix = true)

        when (val result = pattern.match(RequestTarget("/users/42"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42"), result.pathParams)
                assertEquals("/", result.remainingPath)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected prefix match for /users/42" }
            }
        }
    }

    @Test
    fun `testPrefixMatch should return slash as remaining path when matched with trailing slash`() {
        val pattern = PathPattern.parse("/users/{id}", isPrefix = true)

        when (val result = pattern.match(RequestTarget("/users/42/"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42"), result.pathParams)
                assertEquals("/", result.remainingPath)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected prefix match for /users/42/" }
            }
        }
    }

    @Test
    fun `testPrefixMatch should return slash as remaining path when matched with remaining path`() {
        val pattern = PathPattern.parse("/users/{id}", isPrefix = true)

        when (val result = pattern.match(RequestTarget("/users/42/orders"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42"), result.pathParams)
                assertEquals("/orders", result.remainingPath)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected prefix match for /users/42/" }
            }
        }
    }

    @Test
    fun `testPrefixMatch should not match from middle of path`() {
        val pattern = PathPattern.parse("/users", isPrefix = true)

        assertTrue(pattern.match(RequestTarget("/api/users/42")) is PathPatternMatchResult.NoMatch)
    }

    @Test
    fun `testMatch should return true for multiple path parameters in separate segments`() {
        val pattern = PathPattern.parse("/{id}/{name}")

        when (val result = pattern.match(RequestTarget("/42/minthem"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42", "name" to "minthem"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /42/minthem" }
            }
        }
    }

    @Test
    fun `testMatch should allow underscore in parameter name`() {
        val pattern = PathPattern.parse("/users/{user_id}")

        when (val result = pattern.match(RequestTarget("/users/42"))) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("user_id" to "42"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/42" }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "",
            "non-slash-start",
            "/users/{}",
            "/users/{id",
            "/users/id}",
            "/users/foo{id}",
            "/users/{id}bar",
            "/users/{id-name}",
            "/users/{id}{name}",
            "/users/{+++++++}}"
        ]
    )
    fun `testInvalidPathPattern should throw exception for more invalid patterns`(pattern: String) {
        assertFailsWith<IllegalArgumentException> {
            PathPattern.parse(pattern)
        }
    }
}