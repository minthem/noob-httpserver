package io.github.minthem.noobhttpserver.http

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class HttpRequestParserTest {

    @Test
    fun `parse should return a request`() {
        val socketMock = ReadableByteMock.fromStrings(
            listOf(
                "GET /path HTTP/1.1\r\n",
                "Host: localhost\r\n",
                "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                "\r\n"
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024).flip()
        val actual = parser.parse(socketMock, buffer)

        val expected = HttpRequest(
            HttpMethod.GET, "/path", HttpProtocol.HTTP_1_1,
            ImmutableHttpHeaders(
                mapOf(
                    "Host" to listOf("localhost"),
                    "Date" to listOf("Sun Dec 14 19:14:13 JST 2025")
                )
            ),
            InputStream.nullInputStream()
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertEquals(
            0,
            actual.bodyStream.readAllBytes().size,
            "ボディがないリクエストではbodyがnullであることを期待"
        )
    }

    @Test
    fun `parse should return a request when input is split across multiple reads`() {
        val socketMock = ReadableByteMock.fromStrings(
            listOf(
                "GET /pat",
                "h HTTP/1.1\r\nHos",
                "t: localhost\r\nDate: ",
                "Sun Dec 14 19:14",
                ":13 JST 2025\r",
                "\n\r\n"
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024).flip()
        val actual = parser.parse(socketMock, buffer)

        val expected = HttpRequest(
            HttpMethod.GET, "/path", HttpProtocol.HTTP_1_1,
            ImmutableHttpHeaders(
                mapOf(
                    "Host" to listOf("localhost"),
                    "Date" to listOf("Sun Dec 14 19:14:13 JST 2025")
                )
            ),
            InputStream.nullInputStream()
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertEquals(
            0,
            actual.bodyStream.readAllBytes().size,
            "入力が分割されてもボディなしの場合はbodyがnullであることを期待"
        )
    }

    @Test
    fun `parse should return a request with body`() {
        val socketMock = ReadableByteMock.fromStrings(
            listOf(
                "POST /path HTTP/1.1\r\n",
                "Host: localhost\r\n",
                "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                "Content-Length: 11\r\n",
                "\r\n",
                "Hello World"
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024).flip()
        val actual = parser.parse(socketMock, buffer)

        val expected = HttpRequest(
            HttpMethod.POST, "/path", HttpProtocol.HTTP_1_1,
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
        assertContentEquals(
            expected.bodyStream.readAllBytes(),
            actual.bodyStream.readAllBytes(),
            "Content-Length分のボディが読み取れていることを期待"
        )
    }

    @Test
    fun `parse should return a request with body is split across multiple reads`() {
        val socketMock = ReadableByteMock.fromStrings(
            listOf(
                "POST /pat",
                "h HTTP/1.1\r\nHos",
                "t: localhost\r\nDate: ",
                "Sun Dec 14 19:14",
                ":13 JST 2025\r",
                "\nContent-Length: 11\r\n\r\nHello ",
                "World"
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024).flip()
        val actual = parser.parse(socketMock, buffer)

        val expected = HttpRequest(
            HttpMethod.POST, "/path", HttpProtocol.HTTP_1_1,
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
        assertContentEquals(
            expected.bodyStream.readAllBytes(),
            actual.bodyStream.readAllBytes(),
            "ボディが複数readに分割されても連結して読み取れることを期待"
        )
    }

    @Test
    fun `parse should return a request with transfer-encoding chunked`() {
        val socketMock = ReadableByteMock.fromStrings(
            listOf(
                "POST /path HTTP/1.1\r\n",
                "Host: localhost\r\n",
                "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                "Transfer-Encoding: chunked\r\n",
                "\r\n",
                "5\r\nHello\r\n",
                "A\r\nHello Worl\r\n",
                "1\r\nd\r\n",
                "0\r\n\r\n"
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024).flip()
        val actual = parser.parse(socketMock, buffer)

        val expected = HttpRequest(
            HttpMethod.POST, "/path", HttpProtocol.HTTP_1_1,
            ImmutableHttpHeaders(
                mapOf(
                    "Host" to listOf("localhost"),
                    "Date" to listOf("Sun Dec 14 19:14:13 JST 2025"),
                    "Transfer-Encoding" to listOf("chunked")
                )
            ),
            bodyStream = ByteArrayInputStream("HelloHello World".toByteArray(Charsets.US_ASCII))
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertContentEquals(
            expected.bodyStream.readAllBytes(),
            actual.bodyStream.readAllBytes(),
        )
    }


    @Test
    fun `parse should return a request with transfer-encoding chunked split across multiple reads`() {
        val socketMock = ReadableByteMock.fromStrings(
            listOf(
                "POST /path HTTP/1.1\r\n",
                "Host: localhost\r\n",
                "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                "Transfer-Encoding: chunked\r\n",
                "\r\n",
                "5\r",
                "\nHel",
                "lo\r\n",
                "A",
                "\r\nHello Worl\r",
                "\n",
                "1\r",
                "\nd\r\n",
                "0\r\n\r",
                "\n"
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024).flip()
        val actual = parser.parse(socketMock, buffer)

        val expected = HttpRequest(
            HttpMethod.POST, "/path", HttpProtocol.HTTP_1_1,
            ImmutableHttpHeaders(
                mapOf(
                    "Host" to listOf("localhost"),
                    "Date" to listOf("Sun Dec 14 19:14:13 JST 2025"),
                    "Transfer-Encoding" to listOf("chunked")
                )
            ),
            bodyStream = ByteArrayInputStream("HelloHello World".toByteArray(Charsets.US_ASCII))
        )

        assertRequestEqualsIgnoringBody(expected, actual)
        assertContentEquals(
            expected.bodyStream.readAllBytes(),
            actual.bodyStream.readAllBytes(),
        )
    }

    @Test
    fun `parse should throw an exception when header name is invalid`() {
        val socketMock = ReadableByteMock.fromStrings(
            listOf(
                "GET /path HTTP/1.1\r\n",
                "Invalid Host: localhost\r\n",
                "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                "\r\n"
            )
        )
        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024).flip()
        assertThrows<IllegalArgumentException> { parser.parse(socketMock, buffer) }
    }

    @Test
    fun `parse should throw an exception when header value is invalid`() {
        val socketMock = ReadableByteMock.fromStrings(
            listOf(
                "GET /path HTTP/1.1\r\n",
                "Host: Invalid Value\r\r\n",
                "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                "\r\n"
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024).flip()
        assertThrows<IllegalArgumentException> { parser.parse(socketMock, buffer) }
    }

    @Test
    fun `parse should throw an exception when Content-Length and Transfer-Encoding headers are mutually exclusive`() {
        val socketMock = ReadableByteMock.fromStrings(
            listOf(
                "POST /path HTTP/1.1\r\n",
                "Host: localhost\r\n",
                "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                "Transfer-Encoding: chunked\r\n",
                "Content-Length: 36\r\n",
                "\r\n",
                "5\r\nHello\r\n",
                "A\r\nHello Worl\r\n",
                "1\r\nd\r\n",
                "0\r\n\r\n"
            )
        )

        val parser = HttpRequestParser()
        val buffer = ByteBuffer.allocate(1024).flip()
        assertThrows<IllegalStateException> { parser.parse(socketMock, buffer) }
    }

    fun assertRequestEqualsIgnoringBody(expected: HttpRequest, actual: HttpRequest) {
        assertEquals(expected.method, actual.method, "HTTPメソッドが一致することを期待")
        assertEquals(expected.path, actual.path, "パスが一致することを期待")
        assertEquals(expected.protocol, actual.protocol, "プロトコル(HTTPバージョン)が一致することを期待")
        assertEquals(expected.headers, actual.headers, "ヘッダーが一致することを期待")
    }
}