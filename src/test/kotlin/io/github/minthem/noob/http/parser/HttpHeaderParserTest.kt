package io.github.minthem.noob.http.parser

import io.github.minthem.noob.http.config.HttpLimitsConfig
import io.github.minthem.noob.http.io.ByteChannelReadStream
import io.github.minthem.noob.http.testutil.FixedReadableByteChannel
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpHeadersParserTest {

    @Test
    fun `parse accepts headers when sizes are exactly at limits`() {
        val parser = HttpHeadersParser(
            HttpLimitsConfig(
                maxRequestTargetBytes = 8192,
                maxHeaderSectionBytes = 64,
                maxHeaderNameBytes = 4,
                maxHeaderValueBytes = 6,
                maxHeaderCount = 1
            )
        )

        val stream = streamOf(
            "Host: abcde\r\n",
            "\r\n"
        )

        val actual = parser.parse(stream)

        assertEquals("abcde", actual["host"])
    }

    @Test
    fun `parse throws when total header bytes exceed limit`() {
        val parser = HttpHeadersParser(
            HttpLimitsConfig(
                maxRequestTargetBytes = 8192,
                maxHeaderSectionBytes = 10,
                maxHeaderNameBytes = 100,
                maxHeaderValueBytes = 100,
                maxHeaderCount = 10
            )
        )

        val stream = streamOf(
            "Host: localhost\r\n",
            "\r\n"
        )

        val actual = assertFailsWith<IllegalArgumentException> {
            parser.parse(stream)
        }

        assertEquals("Too many header bytes", actual.message)
    }

    @Test
    fun `parse throws when header name bytes exceed limit`() {
        val parser = HttpHeadersParser(
            HttpLimitsConfig(
                maxRequestTargetBytes = 8192,
                maxHeaderSectionBytes = 100,
                maxHeaderNameBytes = 3,
                maxHeaderValueBytes = 100,
                maxHeaderCount = 10
            )
        )

        val stream = streamOf(
            "Host: value\r\n",
            "\r\n"
        )

        val actual = assertFailsWith<IllegalArgumentException> {
            parser.parse(stream)
        }

        assertEquals("Too many header name bytes", actual.message)
    }

    @Test
    fun `parse throws when header value bytes exceed limit`() {
        val parser = HttpHeadersParser(
            HttpLimitsConfig(
                maxRequestTargetBytes = 8192,
                maxHeaderSectionBytes = 100,
                maxHeaderNameBytes = 100,
                maxHeaderValueBytes = 4,
                maxHeaderCount = 10
            )
        )

        val stream = streamOf(
            "Host: value\r\n",
            "\r\n"
        )

        val actual = assertFailsWith<IllegalArgumentException> {
            parser.parse(stream)
        }

        assertEquals("Too many header value bytes", actual.message)
    }

    @Test
    fun `parse throws when header count exceeds limit`() {
        val parser = HttpHeadersParser(
            HttpLimitsConfig(
                maxRequestTargetBytes = 8192,
                maxHeaderSectionBytes = 100,
                maxHeaderNameBytes = 100,
                maxHeaderValueBytes = 100,
                maxHeaderCount = 1
            )
        )

        val stream = streamOf(
            "Host: localhost\r\n",
            "Accept: text/plain\r\n",
            "\r\n"
        )

        val actual = assertFailsWith<IllegalArgumentException> {
            parser.parse(stream)
        }

        assertEquals("Too many headers", actual.message)
    }

    private fun streamOf(vararg chunks: String): ByteChannelReadStream {
        val channel = FixedReadableByteChannel.fromStrings(chunks.toList())
        val buffer = ByteBuffer.allocate(1024)
        buffer.flip()
        return ByteChannelReadStream(channel, buffer)
    }
}