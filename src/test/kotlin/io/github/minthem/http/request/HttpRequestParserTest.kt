package io.github.minthem.http.request

import io.github.minthem.http.header.ImmutableHttpHeaders
import org.junit.jupiter.api.assertThrows
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
            EmptyRequestBody()
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertEquals(expected.body.contentLength(), actual.body.contentLength(), "ボディがないリクエストではbodyがnullであることを期待")
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
            EmptyRequestBody()
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertEquals(expected.body.contentLength(), actual.body.contentLength(), "入力が分割されてもボディなしの場合はbodyがnullであることを期待")
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
            InMemoryRequestBody("Hello World".toByteArray(Charsets.US_ASCII))
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertContentEquals(
            expected.body.openStream().use { it.readAllBytes() },
            actual.body.openStream().use { it.readAllBytes() },
            "Content-Length分のボディが読み取れていることを期待"
        )
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
            InMemoryRequestBody("Hello World".toByteArray(Charsets.US_ASCII))
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertContentEquals(
            expected.body.openStream().use { it.readAllBytes() },
            actual.body.openStream().use { it.readAllBytes() },
            "ボディが複数readに分割されても連結して読み取れることを期待"
        )
    }

    @Test
    fun `parse should throw an exception when header name is invalid`() {
        val socketMock = ReadableByteMock(
            listOf(
                "GET /path HTTP/1.1\r\n".toByteArray(Charsets.US_ASCII),
                "Invalid Host: localhost\r\n".toByteArray(Charsets.US_ASCII),
                "Date: Sun Dec 14 19:14:13 JST 2025\r\n".toByteArray(Charsets.US_ASCII),
                "\r\n".toByteArray(Charsets.US_ASCII)
            )
        )
        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024)
        assertThrows<IllegalArgumentException> { parser.parse(socketMock, buffer) }
    }

    @Test
    fun `parse should throw an exception when header value is invalid`() {
        val socketMock = ReadableByteMock(
            listOf(
                "GET /path HTTP/1.1\r\n".toByteArray(Charsets.US_ASCII),
                "Host: Invalid Value\r\r\n".toByteArray(Charsets.US_ASCII),
                "Date: Sun Dec 14 19:14:13 JST 2025\r\n".toByteArray(Charsets.US_ASCII),
                "\r\n".toByteArray(Charsets.US_ASCII)
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024)
        assertThrows<IllegalArgumentException> { parser.parse(socketMock, buffer) }
    }

    fun assertRequestEqualsIgnoringBody(expected: HttpRequest, actual: HttpRequest) {
        assertEquals(expected.method, actual.method, "HTTPメソッドが一致することを期待")
        assertEquals(expected.path, actual.path, "パスが一致することを期待")
        assertEquals(expected.protocol, actual.protocol, "プロトコル(HTTPバージョン)が一致することを期待")
        assertEquals(expected.headers, actual.headers, "ヘッダーが一致することを期待")
    }
}