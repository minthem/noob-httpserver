package io.github.minthem.noob.http.message

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
    fun `value should return wire format method name`() {
        assertEquals("GET", HttpMethod.GET.value())
        assertEquals("POST", HttpMethod.POST.value())
        assertEquals("PUT", HttpMethod.PUT.value())
        assertEquals("DELETE", HttpMethod.DELETE.value())
        assertEquals("HEAD", HttpMethod.HEAD.value())
    }

    @Test
    fun `fromString should be case sensitive`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                HttpMethod.fromString("get")
            }
        assertEquals("Invalid method: get", exception.message)
    }

    @Test
    fun `fromString should throw an exception when the method is invalid`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                HttpMethod.fromString("INVALID")
            }
        assertEquals("Invalid method: INVALID", exception.message)
    }
}
