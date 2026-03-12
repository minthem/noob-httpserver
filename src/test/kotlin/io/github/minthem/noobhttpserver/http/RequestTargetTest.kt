package io.github.minthem.noobhttpserver.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for the RequestTarget class, which represents HTTP request targets conforming to the RFC 3986 format.
 * It parses a given request target string into `path` and `query` components and validates it using the OriginFormValidator.
 */
internal class RequestTargetTest {

    @Test
    fun `constructor succeeds for valid request target with path only`() {
        val target = "/valid/path"
        val requestTarget = RequestTarget(target)

        assertEquals("/valid/path", requestTarget.rawPath)
        assertNull(requestTarget.rawQuery)
        assertEquals("/valid/path", requestTarget.decodedPath)
        assertEquals(emptyMap(), requestTarget.decodedQuery)
    }

    @Test
    fun `constructor succeeds for valid request target with path and query`() {
        val target = "/valid/path?param1=value1&param2=value2"
        val requestTarget = RequestTarget(target)

        assertEquals("/valid/path", requestTarget.rawPath)
        assertEquals("param1=value1&param2=value2", requestTarget.rawQuery)
        assertEquals("/valid/path", requestTarget.decodedPath)
        assertEquals(
            mapOf(
                "param1" to listOf("value1"),
                "param2" to listOf("value2")
            ),
            requestTarget.decodedQuery
        )
    }

    @Test
    fun `constructor succeeds for valid request target with empty path and query`() {
        val target = ""
        val requestTarget = RequestTarget(target)

        assertEquals("/", requestTarget.rawPath)
        assertNull(requestTarget.rawQuery)
        assertEquals("/", requestTarget.decodedPath)
        assertEquals(emptyMap(), requestTarget.decodedQuery)
    }

    @Test
    fun `constructor succeeds for request target with valid percent-encoded characters in path`() {
        val target = "/some/%20path"
        val requestTarget = RequestTarget(target)

        assertEquals("/some/%20path", requestTarget.rawPath)
        assertNull(requestTarget.rawQuery)
        assertEquals("/some/ path", requestTarget.decodedPath)
        assertEquals(emptyMap(), requestTarget.decodedQuery)
    }

    @Test
    fun `constructor succeeds for request target with query containing reserved characters`() {
        val target = "/path?param1=value1&param2=value2!$&'()*+,;="
        val requestTarget = RequestTarget(target)

        assertEquals("/path", requestTarget.rawPath)
        assertEquals("param1=value1&param2=value2!$&'()*+,;=", requestTarget.rawQuery)
        assertEquals("/path", requestTarget.decodedPath)
        assertEquals(
            mapOf(
                "param1" to listOf("value1"),
                "param2" to listOf("value2!$"),
                "'()* ,;" to listOf("")
            ),
            requestTarget.decodedQuery
        )
    }

    @Test
    fun `constructor succeeds for request target with valid query containing question marks`() {
        val target = "/path?first=1?second=2"
        val requestTarget = RequestTarget(target)

        assertEquals("/path", requestTarget.rawPath)
        assertEquals("first=1?second=2", requestTarget.rawQuery)
        assertEquals("/path", requestTarget.decodedPath)
        assertEquals(
            mapOf(
                "first" to listOf("1?second=2"),
            ),
            requestTarget.decodedQuery
        )
    }

    @Test
    fun `constructor succeeds for request target with trailing slash in path`() {
        val target = "/valid/path/"
        val requestTarget = RequestTarget(target)

        assertEquals("/valid/path/", requestTarget.rawPath)
        assertNull(requestTarget.rawQuery)
        assertEquals("/valid/path/", requestTarget.decodedPath)
        assertEquals(emptyMap(), requestTarget.decodedQuery)
    }

    @Test
    fun `decodedPath should decode percent encoded path`() {
        val requestTarget = RequestTarget("/users/%E3%81%82/profile")

        assertEquals("/users/%E3%81%82/profile", requestTarget.rawPath)
        assertEquals("/users/あ/profile", requestTarget.decodedPath)
    }

    @Test
    fun `decodedQuery should return empty map when query is empty`() {
        val requestTarget = RequestTarget("/path?")

        assertEquals(emptyMap(), requestTarget.decodedQuery)
    }

    @Test
    fun `decodedQuery should group multiple values for same key`() {
        val requestTarget = RequestTarget("/path?a=1&a=2&a=3")

        assertEquals(
            mapOf("a" to listOf("1", "2", "3")),
            requestTarget.decodedQuery
        )
    }

    @Test
    fun `decodedQuery should treat key without value as empty string`() {
        val requestTarget = RequestTarget("/path?a")

        assertEquals(
            mapOf("a" to listOf("")),
            requestTarget.decodedQuery
        )
    }

    @Test
    fun `decodedQuery should treat explicit empty value as empty string`() {
        val requestTarget = RequestTarget("/path?a=")

        assertEquals(
            mapOf("a" to listOf("")),
            requestTarget.decodedQuery
        )
    }

    @Test
    fun `decodedQuery should decode percent encoded keys and values`() {
        val requestTarget = RequestTarget("/path?na%6De=%E3%81%82&q=http%20server")

        assertEquals(
            mapOf(
                "name" to listOf("あ"),
                "q" to listOf("http server")
            ),
            requestTarget.decodedQuery
        )
    }

    @Test
    fun `decodedQuery should ignore blank query segments`() {
        val requestTarget = RequestTarget("/path?a=1&&b=2&")

        assertEquals(
            mapOf(
                "a" to listOf("1"),
                "b" to listOf("2")
            ),
            requestTarget.decodedQuery
        )
    }

    @Test
    fun `decodedQuery should allow empty key`() {
        val requestTarget = RequestTarget("/path?=value")

        assertEquals(
            mapOf("" to listOf("value")),
            requestTarget.decodedQuery
        )
    }
}