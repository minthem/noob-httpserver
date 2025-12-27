package io.github.minthem.noobhttpserver.http

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
    fun `fromString should throw an exception when the protocol is invalid`() {
        assertThrows<IllegalArgumentException> { HttpProtocol.fromString("INVALID") }
    }
}
