package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.testutils.ByteArrayWritableChannel
import io.github.minthem.noobhttpserver.testutils.SideEffectWritableChannel
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpResponseWriterTest {

    private val writer = HttpResponseWriter

    @Test
    fun `writes 200 response without body`() {
        val channel = ByteArrayWritableChannel()
        val response = HttpResponse.build {}
        val now = fixedNow()

        writer.write(channel, HttpProtocol.HTTP_1_1, response, now)

        val actual = writtenText(channel)

        assertTrue(actual.startsWith("HTTP/1.1 200 OK\r\n"))
        assertContains(actual, "date: Sun, 1 Jan 2023 00:00:00 GMT\r\n")
        assertContains(actual, "\r\n\r\n")
        assertTrue(actual.endsWith("\r\n\r\n"))
    }

    @Test
    fun `writes 200 response with body`() {
        val channel = ByteArrayWritableChannel()
        val response = HttpResponse.build {
            body("hello")
        }
        val now = fixedNow()

        writer.write(channel, HttpProtocol.HTTP_1_1, response, now)

        val actual = writtenText(channel)

        assertTrue(actual.startsWith("HTTP/1.1 200 OK\r\n"))
        assertContains(actual, "date: Sun, 1 Jan 2023 00:00:00 GMT\r\n")
        assertContains(actual, "\r\n\r\nhello")
        assertTrue(actual.endsWith("hello"))
    }

    @Test
    fun `writes error status response`() {
        val channel = ByteArrayWritableChannel()
        val response = HttpResponse.build {
            status = HttpStatus.NOT_FOUND
            body("not found")
        }
        val now = fixedNow()

        writer.write(channel, HttpProtocol.HTTP_1_1, response, now)

        val actual = writtenText(channel)

        assertTrue(actual.startsWith("HTTP/1.1 404 Not Found\r\n"))
        assertContains(actual, "\r\n\r\nnot found")
    }

    @Test
    fun `writes custom headers`() {
        val channel = ByteArrayWritableChannel()
        val response = HttpResponse.build {
            header("X-Test", "value")
            body("hello")
        }
        val now = fixedNow()

        writer.write(channel, HttpProtocol.HTTP_1_1, response, now)

        val actual = writtenText(channel)

        assertContains(actual, "x-test: value\r\n")
    }

    @Test
    fun `writes repeated headers as multiple lines`() {
        val channel = ByteArrayWritableChannel()
        val response = HttpResponse.build {
            header("Set-Cookie", "a=1")
            header("Set-Cookie", "b=2")
            body("ok")
        }
        val now = fixedNow()

        writer.write(channel, HttpProtocol.HTTP_1_1, response, now)

        val actual = writtenText(channel)

        assertContains(actual, "set-cookie: a=1\r\n")
        assertContains(actual, "set-cookie: b=2\r\n")
    }

    @Test
    fun `adds date header when missing`() {
        val channel = ByteArrayWritableChannel()
        val response = HttpResponse.build {
            body("hello")
        }
        val now = ZonedDateTime.of(2023, 1, 1, 9, 0, 0, 0, ZoneId.of("Asia/Tokyo"))

        writer.write(channel, HttpProtocol.HTTP_1_1, response, now)

        val actual = writtenText(channel)

        assertContains(actual, "date: Sun, 1 Jan 2023 00:00:00 GMT\r\n")
    }

    @Test
    fun `does not overwrite existing date header`() {
        val channel = ByteArrayWritableChannel()
        val response = HttpResponse.build {
            header("date", "Mon, 2 Jan 2023 00:00:00 GMT")
            body("hello")
        }
        val now = fixedNow()

        writer.write(channel, HttpProtocol.HTTP_1_1, response, now)

        val actual = writtenText(channel)

        assertContains(actual, "date: Mon, 2 Jan 2023 00:00:00 GMT\r\n")
        assertFalse(actual.contains("date: Sun, 1 Jan 2023 00:00:00 GMT\r\n"))
    }

    @Test
    fun `writes long header value across buffer boundaries`() {
        val channel = ByteArrayWritableChannel()
        val longValue = "a".repeat(3000)
        val response = HttpResponse.build {
            header("X-Long", longValue)
            body("ok")
        }
        val now = fixedNow()

        writer.write(channel, HttpProtocol.HTTP_1_1, response, now)

        val actual = writtenText(channel)

        assertContains(actual, "x-long: $longValue\r\n")
        assertContains(actual, "\r\n\r\nok")
    }

    @Test
    fun `writes complete response even when partial writes occur`() {
        val channel = ByteArrayWritableChannel()
        val bodyText = "x".repeat(2000)
        val response = HttpResponse.build {
            header("X-Test", "value")
            body(bodyText)
        }
        val now = fixedNow()

        writer.write(channel, HttpProtocol.HTTP_1_1, response, now)

        val actual = writtenText(channel)

        assertTrue(actual.startsWith("HTTP/1.1 200 OK\r\n"))
        assertContains(actual, "x-test: value\r\n")
        assertContains(actual, "\r\n\r\n")
        assertTrue(actual.endsWith(bodyText))
    }

    @Test
    fun `writes HTTP 1_0 status line`() {
        val channel = ByteArrayWritableChannel()
        val response = HttpResponse.build {
            body("hello")
        }
        val now = fixedNow()

        writer.write(channel, HttpProtocol.HTTP_1_0, response, now)

        val actual = writtenText(channel)

        assertTrue(actual.startsWith("HTTP/1.0 200 OK\r\n"))
        assertContains(actual, "\r\n\r\nhello")
    }

    @Test
    fun `propagates exception when channel write fails`() {
        val channel = SideEffectWritableChannel { throw IOException("boom") }
        val response = HttpResponse.build {
            body("hello")
        }
        val now = fixedNow()

        val actual = assertFailsWith<IOException> {
            writer.write(channel, HttpProtocol.HTTP_1_1, response, now)
        }

        assertTrue(actual.message!!.contains("boom"))
    }

    @Test
    fun `throws IllegalStateException when channel returns negative one`() {
        val channel = SideEffectWritableChannel { -1 }
        val response = HttpResponse.build {
            body("hello")
        }
        val now = fixedNow()

        val actual = assertFailsWith<IllegalStateException> {
            writer.write(channel, HttpProtocol.HTTP_1_1, response, now)
        }

        assertTrue(actual.message!!.contains("Unexpected end of stream"))
    }

    @Test
    fun `propagates exception when body write fails`() {
        var writeCount = 0
        val channel = SideEffectWritableChannel { buffer ->
            writeCount++

            // 1回目はstatusLine + headersの書き込み、2回目以降はボディへの書き込みを想定
            if (writeCount == 1) {
                val remaining = buffer?.remaining() ?: 0
                buffer?.position(buffer.position() + remaining)
                remaining
            } else {
                throw IOException("body write failed")
            }
        }

        val response = HttpResponse.build {
            body("hello")
        }
        val now = fixedNow()

        val actual = assertFailsWith<IOException> {
            writer.write(channel, HttpProtocol.HTTP_1_1, response, now)
        }

        assertTrue(actual.message!!.contains("body write failed"))
    }

    private fun writtenText(channel: ByteArrayWritableChannel): String {
        return channel.toByteArray().toString(Charsets.UTF_8)
    }

    private fun fixedNow(): ZonedDateTime {
        return ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
    }
}