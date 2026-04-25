package io.github.minthem.noob.http.message

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpProtocolTest {
    @Test
    fun `fromString should return a valid HttpProtocol`() {
        assertEquals(HttpProtocol.HTTP_1_0, HttpProtocol.fromString("HTTP/1.0"))
        assertEquals(HttpProtocol.HTTP_1_1, HttpProtocol.fromString("HTTP/1.1"))
    }

    @Test
    fun `version should return wire format protocol version`() {
        assertEquals("HTTP/1.0", HttpProtocol.HTTP_1_0.version())
        assertEquals("HTTP/1.1", HttpProtocol.HTTP_1_1.version())
    }

    @Test
    fun `fromString should be case sensitive`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                HttpProtocol.fromString("http/1.1")
            }
        assertEquals("Invalid protocol: http/1.1", exception.message)
    }

    @Test
    fun `fromString should throw an exception when the protocol is invalid`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                HttpProtocol.fromString("INVALID")
            }
        assertEquals("Invalid protocol: INVALID", exception.message)
    }
}
