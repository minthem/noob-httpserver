package io.github.minthem.http.request

import io.github.minthem.http.header.ImmutableHttpHeaders
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class HttpRequestParserTest {

    @Test
    fun `parse should return a request`() {
        val socketMock = ReadableByteMock(
            listOf(
                "GET /path HTTP/1.1\r\n".toByteArray(Charsets.US_ASCII),
                "Host: localhost\r\n".toByteArray(Charsets.US_ASCII),
                "Date: Sun Dec 14 19:14:13 JST 2025\r\n".toByteArray(Charsets.US_ASCII),
                "\r\n".toByteArray(Charsets.US_ASCII)
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024)
        val actual = parser.parse(socketMock, buffer)

        val expected = HttpRequest(
            "GET", "/path", "HTTP/1.1",
            ImmutableHttpHeaders(
                mapOf(
                    "Host" to listOf("localhost"),
                    "Date" to listOf("Sun Dec 14 19:14:13 JST 2025")
                )
            ),
            null
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertEquals(expected.body, actual.body)
    }

    @Test
    fun `parse should return a request when input is split across multiple reads`() {
        val socketMock = ReadableByteMock(
            listOf(
                "GET /pat".toByteArray(Charsets.US_ASCII),
                "h HTTP/1.1\r\nHos".toByteArray(Charsets.US_ASCII),
                "t: localhost\r\nDate: ".toByteArray(Charsets.US_ASCII),
                "Sun Dec 14 19:14".toByteArray(Charsets.US_ASCII),
                ":13 JST 2025\r".toByteArray(Charsets.US_ASCII),
                "\n\r\n".toByteArray(Charsets.US_ASCII)
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024)
        val actual = parser.parse(socketMock, buffer)

        val expected = HttpRequest(
            "GET", "/path", "HTTP/1.1",
            ImmutableHttpHeaders(
                mapOf(
                    "Host" to listOf("localhost"),
                    "Date" to listOf("Sun Dec 14 19:14:13 JST 2025")
                )
            ),
            null
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertEquals(expected.body, actual.body)
    }

    @Test
    fun `parse should return a request with body`() {
        val socketMock = ReadableByteMock(
            listOf(
                "POST /path HTTP/1.1\r\n".toByteArray(Charsets.US_ASCII),
                "Host: localhost\r\n".toByteArray(Charsets.US_ASCII),
                "Date: Sun Dec 14 19:14:13 JST 2025\r\n".toByteArray(Charsets.US_ASCII),
                "Content-Length: 11\r\n".toByteArray(Charsets.US_ASCII),
                "\r\n".toByteArray(Charsets.US_ASCII),
                "Hello World".toByteArray(Charsets.US_ASCII)
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024)
        val actual = parser.parse(socketMock, buffer)

        val expected = HttpRequest(
            "POST", "/path", "HTTP/1.1",
            ImmutableHttpHeaders(
                mapOf(
                    "Host" to listOf("localhost"),
                    "Date" to listOf("Sun Dec 14 19:14:13 JST 2025"),
                    "Content-Length" to listOf("11")
                )
            ),
            ByteArrayInputStream("Hello World".toByteArray(Charsets.US_ASCII))
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertContentEquals(expected.body?.readAllBytes(), actual.body?.readAllBytes())
    }


    @Test
    fun `parse should return a request with body is split across multiple reads`() {
        val socketMock = ReadableByteMock(
            listOf(
                "POST /pat".toByteArray(Charsets.US_ASCII),
                "h HTTP/1.1\r\nHos".toByteArray(Charsets.US_ASCII),
                "t: localhost\r\nDate: ".toByteArray(Charsets.US_ASCII),
                "Sun Dec 14 19:14".toByteArray(Charsets.US_ASCII),
                ":13 JST 2025\r".toByteArray(Charsets.US_ASCII),
                "\nContent-Length: 11\r\n\r\nHello ".toByteArray(Charsets.US_ASCII),
                "World".toByteArray(Charsets.US_ASCII)
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024)
        val actual = parser.parse(socketMock, buffer)

        val expected = HttpRequest(
            "POST", "/path", "HTTP/1.1",
            ImmutableHttpHeaders(
                mapOf(
                    "Host" to listOf("localhost"),
                    "Date" to listOf("Sun Dec 14 19:14:13 JST 2025"),
                    "Content-Length" to listOf("11")
                )
            ),
            ByteArrayInputStream("Hello World".toByteArray(Charsets.US_ASCII))
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertContentEquals(expected.body?.readAllBytes(), actual.body?.readAllBytes())
    }

    fun assertRequestEqualsIgnoringBody(expected: HttpRequest, actual: HttpRequest) {
        assertEquals(expected.method, actual.method)
        assertEquals(expected.path, actual.path)
        assertEquals(expected.protocol, actual.protocol)
        assertEquals(expected.headers, actual.headers)
    }
}