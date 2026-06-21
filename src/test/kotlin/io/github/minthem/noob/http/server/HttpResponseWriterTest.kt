package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.config.BufferConfig
import io.github.minthem.noob.http.message.BodyWriter
import io.github.minthem.noob.http.message.FallbackRequestMetadata
import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.HttpResponsePreparer
import io.github.minthem.noob.http.message.HttpStatus
import io.github.minthem.noob.http.message.MutableHttpHeaders
import io.github.minthem.noob.http.message.PreparedHttpResponse
import io.github.minthem.noob.http.testutil.ByteArrayWritableChannel
import io.github.minthem.noob.http.testutil.SideEffectWritableChannel
import io.github.minthem.noob.http.util.asCloseable
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HttpResponseWriterTest {
    private val config = BufferConfig()
    private val writer = HttpResponseWriter(config.responseHeaderBytes)

    @Test
    fun `writes 200 response without body`() {
        val channel = ByteArrayWritableChannel()
        val prepared = createPreparedResponse()

        writer.write(channel, prepared)

        val actual = writtenText(channel)

        assertTrue(actual.startsWith("HTTP/1.1 200 OK\r\n"))
        assertContains(actual, "\r\n\r\n")
        assertTrue(actual.endsWith("\r\n\r\n"))
    }

    @Test
    fun `writes 200 response with body`() {
        val channel = ByteArrayWritableChannel()
        val prepared =
            createPreparedResponse(
                bodyWriter = stringBodyWriter("hello"),
            )

        writer.write(channel, prepared)

        val actual = writtenText(channel)

        assertTrue(actual.startsWith("HTTP/1.1 200 OK\r\n"))
        assertContains(actual, "\r\n\r\nhello")
        assertTrue(actual.endsWith("hello"))
    }

    @Test
    fun `writes error status response`() {
        val channel = ByteArrayWritableChannel()
        val prepared =
            createPreparedResponse(
                status = HttpStatus.NOT_FOUND,
                bodyWriter = stringBodyWriter("not found"),
            )

        writer.write(channel, prepared)

        val actual = writtenText(channel)

        assertTrue(actual.startsWith("HTTP/1.1 404 Not Found\r\n"))
        assertContains(actual, "\r\n\r\nnot found")
    }

    @Test
    fun `writes custom headers`() {
        val channel = ByteArrayWritableChannel()
        val headers = MutableHttpHeaders().apply { add("X-Test", "value") }
        val prepared =
            createPreparedResponse(
                headers = headers,
                bodyWriter = stringBodyWriter("hello"),
            )

        writer.write(channel, prepared)

        val actual = writtenText(channel)

        assertContains(actual, "x-test: value\r\n")
    }

    @Test
    fun `writes repeated headers as multiple lines`() {
        val channel = ByteArrayWritableChannel()
        val headers =
            MutableHttpHeaders().apply {
                add("Set-Cookie", "a=1")
                add("Set-Cookie", "b=2")
            }
        val prepared =
            createPreparedResponse(
                headers = headers,
                bodyWriter = stringBodyWriter("ok"),
            )

        writer.write(channel, prepared)

        val actual = writtenText(channel)

        assertContains(actual, "set-cookie: a=1\r\n")
        assertContains(actual, "set-cookie: b=2\r\n")
    }

    @Test
    fun `writes long header value across buffer boundaries`() {
        val channel = ByteArrayWritableChannel()
        val longValue = "a".repeat(3000)
        val headers = MutableHttpHeaders().apply { add("X-Long", longValue) }
        val prepared =
            createPreparedResponse(
                headers = headers,
                bodyWriter = stringBodyWriter("ok"),
            )

        writer.write(channel, prepared)

        val actual = writtenText(channel)

        assertContains(actual, "x-long: $longValue\r\n")
        assertContains(actual, "\r\n\r\nok")
    }

    @Test
    fun `writes complete response even when partial writes occur`() {
        val channel = ByteArrayWritableChannel()
        val bodyText = "x".repeat(2000)
        val headers = MutableHttpHeaders().apply { add("X-Test", "value") }

        val response =
            HttpResponse.build {
                header(headers)
                body(bodyText)
            }
        val preparer = HttpResponsePreparer()
        val prepared = preparer.prepare(FallbackRequestMetadata(), response)

        writer.write(channel, prepared)

        val actual = writtenText(channel)

        assertTrue(actual.startsWith("HTTP/1.1 200 OK\r\n"))
        assertContains(actual, "x-test: value\r\n")
        assertContains(actual, "\r\n\r\n")
        assertTrue(actual.endsWith(bodyText))
    }

    @Test
    fun `writes chunked format correctly`() {
        val channel = ByteArrayWritableChannel()

        // チャンクのシーケンスを生成
        val chunkSizes = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 94)
        val chunkSequence =
            chunkSizes
                .asSequence()
                .map { "c".repeat(it).toByteArray(Charsets.UTF_8) }
                .asCloseable { }

        val response =
            HttpResponse.build {
                body(chunkSequence)
            }
        val preparer = HttpResponsePreparer()
        val prepared = preparer.prepare(FallbackRequestMetadata(), response)

        writer.write(channel, prepared)

        val actual = writtenText(channel)

        assertTrue(actual.startsWith("HTTP/1.1 200 OK\r\n"))
        assertContains(actual, "transfer-encoding: chunked\r\n")

        // ヘッダーとボディの境界を特定してボディ部分を抽出
        val bodyStartIndex = actual.indexOf("\r\n\r\n") + 4
        val chunkedBody = actual.substring(bodyStartIndex)

        // 期待されるチャンクボディを組み立てる
        val expectedChunkedBody = StringBuilder()
        chunkSizes.forEach { size ->
            expectedChunkedBody.append(size.toString(16)).append("\r\n")
            expectedChunkedBody.append("c".repeat(size)).append("\r\n")
        }
        expectedChunkedBody.append("0\r\n\r\n")

        assertEquals(expectedChunkedBody.toString(), chunkedBody)
    }

    @Test
    fun `writes HTTP 1_0 status line`() {
        val channel = ByteArrayWritableChannel()
        val prepared =
            createPreparedResponse(
                protocol = HttpProtocol.HTTP_1_0,
                bodyWriter = stringBodyWriter("hello"),
            )

        writer.write(channel, prepared)

        val actual = writtenText(channel)

        assertTrue(actual.startsWith("HTTP/1.0 200 OK\r\n"))
        assertContains(actual, "\r\n\r\nhello")
    }

    @Test
    fun `propagates exception when channel write fails`() {
        val channel = SideEffectWritableChannel { throw IOException("boom") }
        val prepared =
            createPreparedResponse(
                bodyWriter = stringBodyWriter("hello"),
            )

        val actual =
            assertFailsWith<IOException> {
                writer.write(channel, prepared)
            }

        assertTrue(actual.message!!.contains("boom"))
    }

    @Test
    fun `throws IllegalStateException when channel returns negative one`() {
        val channel = SideEffectWritableChannel { -1 }
        val prepared =
            createPreparedResponse(
                bodyWriter = stringBodyWriter("hello"),
            )

        val actual =
            assertFailsWith<IllegalStateException> {
                writer.write(channel, prepared)
            }

        assertTrue(actual.message!!.contains("Unexpected end of stream"))
    }

    @Test
    fun `propagates exception when body write fails`() {
        var writeCount = 0
        val channel =
            SideEffectWritableChannel { buffer ->
                writeCount++
                if (writeCount == 1) {
                    val remaining = buffer?.remaining() ?: 0
                    buffer?.position(buffer.position() + remaining)
                    remaining
                } else {
                    throw IOException("body write failed")
                }
            }

        val failingBodyWriter =
            object : BodyWriter {
                override fun write(destination: WritableByteChannel) {
                    destination.write(ByteBuffer.wrap("a".toByteArray())) // causes error in SideEffectWritableChannel
                }
            }

        val prepared = createPreparedResponse(bodyWriter = failingBodyWriter)

        val actual =
            assertFailsWith<IOException> {
                writer.write(channel, prepared)
            }

        assertTrue(actual.message!!.contains("body write failed"))
    }

    private fun writtenText(channel: ByteArrayWritableChannel): String = channel.toByteArray().toString(Charsets.UTF_8)

    private fun createPreparedResponse(
        protocol: HttpProtocol = HttpProtocol.HTTP_1_1,
        status: HttpStatus = HttpStatus.OK,
        headers: HttpHeaders = HttpHeaders.EMPTY,
        bodyWriter: BodyWriter =
            object : BodyWriter {
                override fun write(destination: WritableByteChannel) {}
            },
    ): PreparedHttpResponse = PreparedHttpResponse(protocol, status, headers, bodyWriter)

    private fun stringBodyWriter(content: String): BodyWriter =
        object : BodyWriter {
            override fun write(destination: WritableByteChannel) {
                destination.write(ByteBuffer.wrap(content.toByteArray(Charsets.UTF_8)))
            }
        }
}
