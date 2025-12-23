package io.github.minthem.noobhttpserver.http

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpMethodTest {

    @Test
    fun `fromString should return a valid HttpMethod`() {
        assertEquals(HttpMethod.GET, HttpMethod.fromString("GET"))
        assertEquals(HttpMethod.POST, HttpMethod.fromString("POST"))
        assertEquals(HttpMethod.PUT, HttpMethod.fromString("PUT"))
        assertEquals(HttpMethod.DELETE, HttpMethod.fromString("DELETE"))
        assertEquals(HttpMethod.HEAD, HttpMethod.fromString("HEAD"))
    }

    @Test
    fun `fromString should throw an exception when the method is invalid`() {
        assertThrows<IllegalArgumentException> { HttpMethod.fromString("INVALID") }

    }
}