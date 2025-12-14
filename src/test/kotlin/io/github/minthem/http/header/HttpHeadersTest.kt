package io.github.minthem.http.header

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class HttpHeadersTest {

    @Test
    fun `getFirst should return first header value when present`() {
        val headers = ImmutableHttpHeaders(mapOf("Content-Type" to listOf("application/json", "text/html")))
        val result = headers.getFirst("Content-Type")
        assertEquals("application/json", result)
    }

    @Test
    fun `getFirst should return null when header does not exist`() {
        val headers = ImmutableHttpHeaders(emptyMap())
        val result = headers.getFirst("Authorization")
        assertNull(result)
    }

    @Test
    fun `get should return list of header values when present`() {
        val headers = ImmutableHttpHeaders(mapOf("Accept" to listOf("text/plain", "text/html")))
        val result = headers["Accept"]
        assertEquals(listOf("text/plain", "text/html"), result)
    }

    @Test
    fun `get should return null when header does not exist`() {
        val headers = ImmutableHttpHeaders(emptyMap())
        val result = headers["Authorization"]
        assertNull(result)
    }

    @Test
    fun `contains should return true when header exists`() {
        val headers = ImmutableHttpHeaders(mapOf("Host" to listOf("example.com")))
        assertTrue("Host" in headers)
    }

    @Test
    fun `contains should return false when header does not exist`() {
        val headers = ImmutableHttpHeaders(emptyMap())
        assertFalse("Authorization" in headers)
    }

    @Test
    fun `add should append value to existing header`() {
        val headers = MutableHttpHeaders(mapOf("Cache-Control" to listOf("no-cache")))
        headers.add("Cache-Control", "no-store")
        assertEquals(listOf("no-cache", "no-store"), headers["Cache-Control"])
    }

    @Test
    fun `add should create a new header if it does not exist`() {
        val headers = MutableHttpHeaders(emptyMap())
        headers.add("Authorization", "Bearer token")
        assertEquals(listOf("Bearer token"), headers["Authorization"])
    }

    @Test
    fun `set should overwrite existing header values`() {
        val headers = MutableHttpHeaders(mapOf("Content-Type" to listOf("text/html", "application/json")))
        headers.set("Content-Type", "application/xml")
        assertEquals(listOf("application/xml"), headers["Content-Type"])
    }

    @Test
    fun `set should create a new header if it does not exist`() {
        val headers = MutableHttpHeaders(emptyMap())
        headers.set("X-Custom-Header", "custom-value")
        assertEquals(listOf("custom-value"), headers["X-Custom-Header"])
    }

    @Test
    fun `remove should delete existing header`() {
        val headers = MutableHttpHeaders(mapOf("ETag" to listOf("12345")))
        headers.remove("ETag")
        assertFalse("ETag" in headers)
    }

    @Test
    fun `remove should do nothing if header does not exist`() {
        val headers = MutableHttpHeaders(emptyMap())
        headers.remove("Non-Existent-Header")
        assertFalse("Non-Existent-Header" in headers)
    }

    @Test
    fun `headers should be case-insensitive`() {
        val headers = MutableHttpHeaders(mapOf("Cache-Control" to listOf("no-cache")))
        assertTrue("cache-control" in headers)
        assertEquals(listOf("no-cache"), headers["CACHE-CONTROL"])
    }

    @Test
    fun `headers equality`() {
        val headers1 = ImmutableHttpHeaders(
            mapOf(
                "Cache-Control" to listOf("no-cache"),
                "Content-Type" to listOf("application/json")
            )
        )
        val headers2 = ImmutableHttpHeaders(
            mapOf(
                "Cache-Control" to listOf("no-cache"),
                "Content-Type" to listOf("application/json")
            )
        )
        assertEquals(headers1, headers2)
    }

    @Test
    fun `headers inequality`() {
        val headers1 = ImmutableHttpHeaders(mapOf("Cache-Control" to listOf("no-cache")))
        val headers2 = ImmutableHttpHeaders(mapOf("Content-Type" to listOf("application/json")))
        assertNotEquals(headers1, headers2)
    }
}