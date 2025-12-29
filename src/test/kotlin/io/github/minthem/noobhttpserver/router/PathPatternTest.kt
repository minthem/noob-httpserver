package io.github.minthem.noobhttpserver.router

import org.junit.jupiter.api.fail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PathPatternTest {

    @Test
    fun `testMatch should return true when path matches pattern`() {
        val pattern = PathPattern("/users/{id}")

        when (val result = pattern.match("/users/42")) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/42" }
            }
        }

        when (val result = pattern.match("/users/42/")) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/42" }
            }
        }

        when (val result = pattern.match("/users/test-user")) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "test-user"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/test-user" }
            }
        }
    }

    @Test
    fun `testMatch should return true when path matches pattern multiple times`() {
        val pattern = PathPattern("/users/{id}/orders/{orderId}")

        when (val result = pattern.match("/users/42/orders/1234567890")) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42", "orderId" to "1234567890"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/42/orders/1234567890" }
            }
        }

        when (val result = pattern.match("/users/42/orders/1234567890/")) {
            is PathPatternMatchResult.Match -> {
                assertEquals(mapOf("id" to "42", "orderId" to "1234567890"), result.pathParams)
            }

            is PathPatternMatchResult.NoMatch -> {
                fail { "Expected match for /users/42/orders/1234567890/" }
            }
        }
    }

    @Test
    fun `testNoMatch should return false when path does not match pattern`() {
        val pattern = PathPattern("/users/{id}")
        assertTrue(pattern.match("/users/") is PathPatternMatchResult.NoMatch)
        assertTrue(pattern.match("/users") is PathPatternMatchResult.NoMatch)
        assertTrue(pattern.match("/orders/42") is PathPatternMatchResult.NoMatch)
    }

    @Test
    fun `testInvalidPathPattern should throw exception for invalid patterns`() {
        assertFailsWith<IllegalArgumentException>("Non slash start") {
            PathPattern("non-slash-start")
        }
        assertFailsWith<IllegalArgumentException>("ブラケットそのまま") { PathPattern("/users/{id}/}") }
        assertFailsWith<IllegalArgumentException>("不正なパス") { PathPattern("/users/?????}") }
        assertFailsWith<IllegalArgumentException>("変数名に使用できない文字列") { PathPattern("/users/{+++++++}}") }
        assertFailsWith<IllegalArgumentException>("セグメント内に複数の変数") { PathPattern("/users/{id}{name}") }
    }
}