package io.github.minthem.noob.http.parser

import io.github.minthem.noob.http.config.HttpLimitsConfig
import io.github.minthem.noob.http.exception.ContentLengthTooLargeException
import io.github.minthem.noob.http.exception.HttpResponseException
import io.github.minthem.noob.http.io.ByteChannelReadStream
import io.github.minthem.noob.http.message.HttpMethod
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.HttpStatus
import io.github.minthem.noob.http.message.ImmutableHttpHeaders
import io.github.minthem.noob.http.message.RequestTarget
import io.github.minthem.noob.http.testutil.FixedReadableByteChannel
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpRequestParserTest {
    private val config = HttpLimitsConfig()
    private val headerParser = HttpHeadersParser(config)

    @Nested
    inner class SuccessTest {
        @Test
        fun `parse should return a request`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                        "\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val actual = parser.parse(channel)

            val expected =
                HttpRequest(
                    HttpMethod.GET,
                    RequestTarget("/path"),
                    HttpProtocol.HTTP_1_1,
                    ImmutableHttpHeaders(
                        mapOf(
                            "Host" to listOf("localhost"),
                            "Date" to listOf("Sun Dec 14 19:14:13 JST 2025"),
                        ),
                    ),
                    InputStream.nullInputStream(),
                )

            assertRequestEqualsIgnoringBody(expected, actual)
            assertEquals(
                0,
                actual.bodyStream.readAllBytes().size,
                "ボディがないリクエストではbodyがnullであることを期待",
            )
        }

        @Test
        fun `parse should return a request when input is split across multiple reads`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /pat",
                        "h HTTP/1.1\r\nHos",
                        "t: localhost\r\nDate: ",
                        "Sun Dec 14 19:14",
                        ":13 JST 2025\r",
                        "\n\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val actual = parser.parse(channel)

            val expected =
                HttpRequest(
                    HttpMethod.GET,
                    RequestTarget("/path"),
                    HttpProtocol.HTTP_1_1,
                    ImmutableHttpHeaders(
                        mapOf(
                            "Host" to listOf("localhost"),
                            "Date" to listOf("Sun Dec 14 19:14:13 JST 2025"),
                        ),
                    ),
                    InputStream.nullInputStream(),
                )

            assertRequestEqualsIgnoringBody(expected, actual)
            assertEquals(
                0,
                actual.bodyStream.readAllBytes().size,
                "入力が分割されてもボディなしの場合はbodyがnullであることを期待",
            )
        }

        @Test
        fun `parse should return a request with body`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "POST /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                        "Content-Length: 11\r\n",
                        "\r\n",
                        "Hello World",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val actual = parser.parse(channel)

            val expected =
                HttpRequest(
                    HttpMethod.POST,
                    RequestTarget("/path"),
                    HttpProtocol.HTTP_1_1,
                    ImmutableHttpHeaders(
                        mapOf(
                            "Host" to listOf("localhost"),
                            "Date" to listOf("Sun Dec 14 19:14:13 JST 2025"),
                            "Content-Length" to listOf("11"),
                        ),
                    ),
                    ByteArrayInputStream("Hello World".toByteArray(Charsets.US_ASCII)),
                )

            assertRequestEqualsIgnoringBody(expected, actual)
            assertContentEquals(
                expected.bodyStream.readAllBytes(),
                actual.bodyStream.readAllBytes(),
                "Content-Length分のボディが読み取れていることを期待",
            )
        }

        @Test
        fun `parse should return a request with body is split across multiple reads`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "POST /pat",
                        "h HTTP/1.1\r\nHos",
                        "t: localhost\r\nDate: ",
                        "Sun Dec 14 19:14",
                        ":13 JST 2025\r",
                        "\nContent-Length: 11\r\n\r\nHello ",
                        "World",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val actual = parser.parse(channel)

            val expected =
                HttpRequest(
                    HttpMethod.POST,
                    RequestTarget("/path"),
                    HttpProtocol.HTTP_1_1,
                    ImmutableHttpHeaders(
                        mapOf(
                            "Host" to listOf("localhost"),
                            "Date" to listOf("Sun Dec 14 19:14:13 JST 2025"),
                            "Content-Length" to listOf("11"),
                        ),
                    ),
                    ByteArrayInputStream("Hello World".toByteArray(Charsets.US_ASCII)),
                )

            assertRequestEqualsIgnoringBody(expected, actual)
            assertContentEquals(
                expected.bodyStream.readAllBytes(),
                actual.bodyStream.readAllBytes(),
                "ボディが複数readに分割されても連結して読み取れることを期待",
            )
        }

        @Test
        fun `parse should return a request with transfer-encoding chunked`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "POST /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                        "Transfer-Encoding: chunked\r\n",
                        "\r\n",
                        "5\r\nHello\r\n",
                        "A\r\nHello Worl\r\n",
                        "1\r\nd\r\n",
                        "0\r\n\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val actual = parser.parse(channel)

            val expected =
                HttpRequest(
                    HttpMethod.POST,
                    RequestTarget("/path"),
                    HttpProtocol.HTTP_1_1,
                    ImmutableHttpHeaders(
                        mapOf(
                            "Host" to listOf("localhost"),
                            "Date" to listOf("Sun Dec 14 19:14:13 JST 2025"),
                            "Transfer-Encoding" to listOf("chunked"),
                        ),
                    ),
                    bodyStream = ByteArrayInputStream("HelloHello World".toByteArray(Charsets.US_ASCII)),
                )

            assertRequestEqualsIgnoringBody(expected, actual)
            assertContentEquals(
                expected.bodyStream.readAllBytes(),
                actual.bodyStream.readAllBytes(),
            )
        }

        @Test
        fun `parse should return a request with transfer-encoding chunked split across multiple reads`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
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
                        "\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val actual = parser.parse(channel)

            val expected =
                HttpRequest(
                    HttpMethod.POST,
                    RequestTarget("/path"),
                    HttpProtocol.HTTP_1_1,
                    ImmutableHttpHeaders(
                        mapOf(
                            "Host" to listOf("localhost"),
                            "Date" to listOf("Sun Dec 14 19:14:13 JST 2025"),
                            "Transfer-Encoding" to listOf("chunked"),
                        ),
                    ),
                    bodyStream = ByteArrayInputStream("HelloHello World".toByteArray(Charsets.US_ASCII)),
                )

            assertRequestEqualsIgnoringBody(expected, actual)
            assertContentEquals(
                expected.bodyStream.readAllBytes(),
                actual.bodyStream.readAllBytes(),
            )
        }

        @Test
        fun `parse should return a request with query parameters`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /search?q=kotlin&q=http%20server HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val actual = parser.parse(channel)

            assertEquals(HttpMethod.GET, actual.method)
            assertEquals(RequestTarget("/search?q=kotlin&q=http%20server"), actual.path)
            assertEquals(HttpProtocol.HTTP_1_1, actual.protocol)
            assertEquals(0, actual.bodyStream.readAllBytes().size)
        }

        @Test
        fun `parse should return a request with percent encoded request target`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /users/%E3%81%82/profile HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val actual = parser.parse(channel)

            assertEquals(RequestTarget("/users/%E3%81%82/profile"), actual.path)
            assertEquals("/users/あ/profile", actual.path.decodedPath)
        }

        @Test
        fun `parse should return an empty body stream when Content-Length is zero`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "POST /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Content-Length: 0\r\n",
                        "\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val actual = parser.parse(channel)

            assertEquals(HttpMethod.POST, actual.method)
            assertEquals(RequestTarget("/path"), actual.path)
            assertEquals(HttpProtocol.HTTP_1_1, actual.protocol)
            assertEquals(0, actual.bodyStream.readAllBytes().size)
        }

        @Test
        fun `parse should treat transfer encoding chunked case insensitively`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "POST /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Transfer-Encoding: Chunked\r\n",
                        "\r\n",
                        "5\r\nHello\r\n",
                        "0\r\n\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val actual = parser.parse(channel)

            assertEquals(HttpMethod.POST, actual.method)
            assertContentEquals(
                "Hello".toByteArray(Charsets.US_ASCII),
                actual.bodyStream.readAllBytes(),
            )
        }

        @Test
        fun `parse should read only content length bytes from body`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "POST /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Content-Length: 5\r\n",
                        "\r\n",
                        "HelloWorld",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val actual = parser.parse(channel)

            assertContentEquals(
                "Hello".toByteArray(Charsets.US_ASCII),
                actual.bodyStream.readAllBytes(),
                "Content-Length で指定された分だけ読み取ることを期待",
            )
        }


        @Test
        fun `parse should return a request when body size is exactly at limit`() {
            val bodyLimitConfig = 1024 * 1024
            val largeBody = "a".repeat(bodyLimitConfig)
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "POST /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Content-Length: ${largeBody.length}\r\n",
                        "\r\n",
                        largeBody,
                    ),
                )
            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, HttpLimitsConfig(maxRequestBodyBytes = bodyLimitConfig.toLong()))
            val actual = parser.parse(channel)

            assertContentEquals(
                largeBody.toByteArray(Charsets.US_ASCII),
                actual.bodyStream.readAllBytes(),
            )
        }
    }

    @Nested
    inner class FailureTest {
        @ParameterizedTest
        @ValueSource(strings = ["get", "INVALID", "TOO_LONG_HTTP_METHOD"])
        fun `parse should throw an exception when method is invalid`(method: String) {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "$method /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                        "\r\n",
                    ),
                )
            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status, "HTTPステータスがBAD_REQUESTであること")
            assertEquals("close", exp.httpResponse.headers["Connection"], "Connectionヘッダーがcloseであること")
        }

        @ParameterizedTest
        @ValueSource(strings = ["HTTP/2.0", "HTTP/1.2", "HTTP/123456789", "", "HTTP/1.1\r"])
        fun `parse should throw an exception when protocol is invalid`(protocol: String) {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /path $protocol\r\n",
                        "Host: localhost\r\n",
                        "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                        "\r\n",
                    ),
                )
            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)

            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status, "HTTPステータスがBAD_REQUESTであること")
            assertEquals("close", exp.httpResponse.headers["Connection"], "Connectionヘッダーがcloseであること")
        }

        @Test
        fun `parse should throw an exception when header name is invalid`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /path HTTP/1.1\r\n",
                        "Invalid Host: localhost\r\n",
                        "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                        "\r\n",
                    ),
                )
            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status, "HTTPステータスがBAD_REQUESTであること")
            assertEquals("close", exp.httpResponse.headers["Connection"], "Connectionヘッダーがcloseであること")
        }

        @Test
        fun `parse should throw an exception when header name is too long`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                        "L".repeat(256),
                        ": value\r\n",
                        "\r\n",
                    ),
                )
            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val headerConfig = HttpLimitsConfig(maxHeaderNameBytes = 255)
            val parser = HttpRequestParser(HttpHeadersParser(headerConfig), config)
            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status, "HTTPステータスがBAD_REQUESTであること")
            assertEquals("close", exp.httpResponse.headers["Connection"], "Connectionヘッダーがcloseであること")
        }

        @Test
        fun `parse should throw an exception when header value is invalid`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /path HTTP/1.1\r\n",
                        "Host: Invalid Value\r\r\n",
                        "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                        "\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status, "HTTPステータスがBAD_REQUESTであること")
            assertEquals("close", exp.httpResponse.headers["Connection"], "Connectionヘッダーがcloseであること")
        }

        @Test
        fun `parse should throw an exception when Content-Length and Transfer-Encoding headers are mutually exclusive`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
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
                        "0\r\n\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status, "HTTPステータスがBAD_REQUESTであること")
            assertEquals("close", exp.httpResponse.headers["Connection"], "Connectionヘッダーがcloseであること")
        }

        @Test
        fun `parse should throw an exception when Request-Target is invalid`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /path/with/invalid|char HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                        "\r\n",
                    ),
                )
            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)
            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status, "HTTPステータスがBAD_REQUESTであること")
            assertEquals("close", exp.httpResponse.headers["Connection"], "Connectionヘッダーがcloseであること")
        }

        @Test
        fun `parser should throw an exception when Request-Target is too long`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET ",
                        "/" + "a".repeat(1000),
                        " HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Date: Sun Dec 14 19:14:13 JST 2025\r\n",
                        "\r\n",
                    ),
                )
            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, HttpLimitsConfig(maxRequestTargetBytes = 1000))
            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status, "HTTPステータスがBAD_REQUESTであること")
            assertEquals("close", exp.httpResponse.headers["Connection"], "Connectionヘッダーがcloseであること")
        }

        @Test
        fun `parse should throw an exception when request target and protocol are not separated by space`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /pathHTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)

            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status)
            assertEquals("close", exp.httpResponse.headers["Connection"])
        }

        @Test
        fun `parse should throw an exception when request line ends with LF only`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /path HTTP/1.1\n",
                        "Host: localhost\r\n",
                        "\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)

            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status)
            assertEquals("close", exp.httpResponse.headers["Connection"])
        }

        @Test
        fun `parse should throw an exception when request line ends unexpectedly`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "GET /path HTTP/1.1",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)

            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status)
            assertEquals("close", exp.httpResponse.headers["Connection"])
        }

        @Test
        fun `parse should throw an exception when Content-Length is not numeric`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "POST /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Content-Length: abc\r\n",
                        "\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)

            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status)
            assertEquals("close", exp.httpResponse.headers["Connection"])
        }

        @Test
        fun `parse should throw an exception when Content-Length is negative`() {
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "POST /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Content-Length: -1\r\n",
                        "\r\n",
                    ),
                )

            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, config)

            val exp = assertThrows<HttpResponseException> { parser.parse(channel) }
            assertEquals(HttpStatus.BAD_REQUEST, exp.httpResponse.status)
            assertEquals("close", exp.httpResponse.headers["Connection"])
        }

        @Test
        fun `parse should throw an exception when Content-Length too large`() {
            val bodyLimitConfig = 1024 * 1024
            val largeBody = "a".repeat(bodyLimitConfig + 1)
            val socketMock =
                FixedReadableByteChannel.fromStrings(
                    listOf(
                        "POST /path HTTP/1.1\r\n",
                        "Host: localhost\r\n",
                        "Content-Length: ${largeBody.length}\r\n",
                        "\r\n",
                        largeBody,
                    ),
                )
            val channel = ByteChannelReadStream(socketMock, ByteBuffer.allocate(1024).flip())
            val parser = HttpRequestParser(headerParser, HttpLimitsConfig(maxRequestBodyBytes = bodyLimitConfig.toLong()))

            val exp = assertFailsWith<ContentLengthTooLargeException> {
                parser.parse(channel)
            }

            assertEquals(largeBody.length.toLong(), exp.contentLength)
            assertEquals(bodyLimitConfig.toLong(), exp.limitBytes)
        }
    }

    private fun assertRequestEqualsIgnoringBody(
        expected: HttpRequest,
        actual: HttpRequest,
    ) {
        assertEquals(expected.method, actual.method, "HTTPメソッドが一致することを期待")
        assertEquals(expected.path, actual.path, "パスが一致することを期待")
        assertEquals(expected.protocol, actual.protocol, "プロトコル(HTTPバージョン)が一致することを期待")
        assertEquals(expected.headers, actual.headers, "ヘッダーが一致することを期待")
    }
}
