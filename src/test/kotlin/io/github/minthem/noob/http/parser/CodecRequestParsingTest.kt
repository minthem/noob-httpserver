package io.github.minthem.noob.http.parser

import io.github.minthem.noob.http.codec.CodecRegistry
import io.github.minthem.noob.http.codec.GzipCodec
import io.github.minthem.noob.http.codec.StreamCodec
import io.github.minthem.noob.http.config.HttpLimitsConfig
import io.github.minthem.noob.http.exception.HttpResponseException
import io.github.minthem.noob.http.io.ByteChannelReadStream
import io.github.minthem.noob.http.message.HttpStatus
import io.github.minthem.noob.http.testutil.FixedReadableByteChannel
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CodecRequestParsingTest {
    private val config = HttpLimitsConfig()

    @Test
    fun `parses decoded gzip request body`() {
        val content = "Hello request GZIP"
        val compressed = gzip(content.encodeToByteArray())
        val parser = parser(CodecRegistry(listOf(GzipCodec())))

        val request = parser.parse(requestStream("gzip", compressed))

        assertEquals(content, request.bodyStream.readAllBytes().decodeToString())
    }

    @Test
    fun `accepts identity request body by default`() {
        val content = "Hello identity".encodeToByteArray()

        val request = parser(CodecRegistry()).parse(requestStream("identity", content))

        assertEquals("Hello identity", request.bodyStream.readAllBytes().decodeToString())
    }

    @Test
    fun `rejects unsupported request body encoding`() {
        val exception =
            assertFailsWith<HttpResponseException> {
                parser(CodecRegistry()).parse(requestStream("br", byteArrayOf()))
            }

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, exception.httpResponse.status)
    }

    @Test
    fun `wraps decoders in reverse content encoding order`() {
        val events = mutableListOf<String>()
        val registry = CodecRegistry(listOf(RecordingCodec("first", events), RecordingCodec("second", events)))

        parser(registry).parse(requestStream("first, second", byteArrayOf()))

        assertEquals(listOf("second", "first"), events)
    }

    private fun parser(registry: CodecRegistry): HttpRequestParser = HttpRequestParser(HttpHeadersParser(config), config, registry)

    private fun requestStream(
        encoding: String,
        body: ByteArray,
    ): ByteChannelReadStream {
        val head =
            "POST / HTTP/1.1\r\n" +
                "Content-Encoding: $encoding\r\n" +
                "Content-Length: ${body.size}\r\n\r\n"
        val channel = FixedReadableByteChannel(listOf(head.encodeToByteArray(), body))
        return ByteChannelReadStream(channel, ByteBuffer.allocate(1024).flip())
    }

    private fun gzip(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(bytes) }
        return output.toByteArray()
    }

    private class RecordingCodec(
        override val id: String,
        private val events: MutableList<String>,
    ) : StreamCodec {
        override fun encode(output: OutputStream): OutputStream = output

        override fun decode(input: InputStream): InputStream {
            events.add(id)
            return input
        }
    }
}
