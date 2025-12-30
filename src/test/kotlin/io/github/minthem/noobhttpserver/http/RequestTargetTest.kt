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
    }

    @Test
    fun `constructor succeeds for valid request target with path and query`() {
        val target = "/valid/path?param1=value1&param2=value2"
        val requestTarget = RequestTarget(target)

        assertEquals("/valid/path", requestTarget.rawPath)
        assertEquals("param1=value1&param2=value2", requestTarget.rawQuery)
    }

    @Test
    fun `constructor succeeds for valid request target with empty path and query`() {
        val target = ""
        val requestTarget = RequestTarget(target)

        assertEquals("/", requestTarget.rawPath)
        assertNull(requestTarget.rawQuery)
    }

    @Test
    fun `constructor succeeds for request target with valid percent-encoded characters in path`() {
        val target = "/some/%20path"
        val requestTarget = RequestTarget(target)

        assertEquals("/some/%20path", requestTarget.rawPath)
        assertNull(requestTarget.rawQuery)
    }

    @Test
    fun `constructor succeeds for request target with query containing reserved characters`() {
        val target = "/path?param1=value1&param2=value2!$&'()*+,;="
        val requestTarget = RequestTarget(target)

        assertEquals("/path", requestTarget.rawPath)
        assertEquals("param1=value1&param2=value2!$&'()*+,;=", requestTarget.rawQuery)
    }

    @Test
    fun `constructor succeeds for request target with valid query containing question marks`() {
        val target = "/path?first=1?second=2"
        val requestTarget = RequestTarget(target)

        assertEquals("/path", requestTarget.rawPath)
        assertEquals("first=1?second=2", requestTarget.rawQuery)
    }

    @Test
    fun `constructor succeeds for request target with trailing slash in path`() {
        val target = "/valid/path/"
        val requestTarget = RequestTarget(target)

        assertEquals("/valid/path/", requestTarget.rawPath)
        assertNull(requestTarget.rawQuery)
    }
}