package io.github.minthem.noobhttpserver.http

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the `addAll` function of the `MutableHttpHeaders` class.
 * The `addAll` function adds a list of header values to a specific header key. 
 * If the key already exists, the values are appended to the current list.
 * If the key does not exist, a new header key with the list of values is created.
 */
class MutableHttpHeadersTest {

    @Test
    fun `addAll should append values to an existing header`() {
        val headers = MutableHttpHeaders(mapOf("Content-Type" to listOf("application/json")))
        headers.addAll("Content-Type", listOf("text/html", "text/plain"))

        assertEquals(
            listOf("application/json", "text/html", "text/plain"),
            headers["Content-Type"],
            "addAll must append values to an existing header"
        )
    }

    @Test
    fun `addAll should create a new header if it does not exist`() {
        val headers = MutableHttpHeaders()
        headers.addAll("Accept", listOf("text/html", "text/plain"))

        assertEquals(
            listOf("text/html", "text/plain"),
            headers["Accept"],
            "addAll must create a new header if it does not exist"
        )
    }

    @Test
    fun `addAll should handle an empty list of values and not modify existing headers`() {
        val headers = MutableHttpHeaders(mapOf("Content-Type" to listOf("application/json")))
        headers.addAll("Content-Type", emptyList())

        assertEquals(
            listOf("application/json"),
            headers["Content-Type"],
            "addAll with an empty list must not modify the existing header"
        )
    }

    @Test
    fun `addAll with empty list for non-existent header should create an empty header`() {
        val headers = MutableHttpHeaders()
        headers.addAll("Accept", emptyList())

        assertEquals(
            emptyList(),
            headers["Accept"],
            "addAll with an empty list must create a new, empty header"
        )
    }

    @Test
    fun `addAll should throw exception for invalid header name`() {
        val headers = MutableHttpHeaders()
        assertThrows<IllegalArgumentException> {
            headers.addAll("Invalid Header", listOf("value1", "value2"))
        }
    }

    @Test
    fun `addAll should throw exception for invalid header values`() {
        val headers = MutableHttpHeaders()
        assertThrows<IllegalArgumentException> {
            headers.addAll("Content-Type", listOf("validValue", "Invalid\nValue"))
        }
    }

    @Test
    fun `addAll should work with normalized keys`() {
        val headers = MutableHttpHeaders()
        headers.addAll("Content-Type", listOf("application/json"))
        headers.addAll("content-type", listOf("text/plain"))

        assertEquals(
            listOf("application/json", "text/plain"),
            headers["CONTENT-TYPE"],
            "addAll must be case-insensitive and normalize header keys"
        )
    }

    @Test
    fun `addAll should handle mixed-case header names correctly`() {
        val headers = MutableHttpHeaders()
        headers.addAll("Accept", listOf("text/html"))
        headers.addAll("aCcEpT", listOf("text/plain"))

        assertEquals(
            listOf("text/html", "text/plain"),
            headers["ACCEPT"],
            "addAll must handle mixed-case header names and normalize correctly"
        )
    }

    @Test
    fun `addAll should do nothing if adding an empty list to an empty MutableHttpHeaders`() {
        val headers = MutableHttpHeaders()
        headers.addAll("Non-Existent-Header", emptyList())

        assertFalse(
            headers.values.containsKey("Non-Existent-Header"),
            "addAll should not create a header for an empty list"
        )
    }
}