package io.github.minthem.noob.http.addon

import io.github.minthem.noob.http.config.HttpLimitsConfig
import io.github.minthem.noob.http.exception.BadRequestException
import io.github.minthem.noob.http.exception.HttpResponseException
import io.github.minthem.noob.http.io.ByteChannelReadStream
import io.github.minthem.noob.http.message.BodyProducerFactory
import io.github.minthem.noob.http.message.BodySpec
import io.github.minthem.noob.http.message.ContentNegotiator
import io.github.minthem.noob.http.message.FallbackRequestMetadata
import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.HttpResponsePreparer
import io.github.minthem.noob.http.parser.HttpHeadersParser
import io.github.minthem.noob.http.parser.HttpRequestParser
import io.github.minthem.noob.http.testutil.ByteArrayWritableChannel
import io.github.minthem.noob.http.testutil.FixedReadableByteChannel
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GzipBodySupportTest {
    private val registry = AddonRegistry(listOf(GzipBodySupport()))

    @Test
    fun `should encode response body when gzip is accepted`() {
        val preparer =
            HttpResponsePreparer(
                contentNegotiator = ContentNegotiator(registry.responseBodyEncoders()),
            )
        val request = FallbackRequestMetadata(headers = HttpHeaders.of("Accept-Encoding" to "gzip"))

        val prepared = preparer.prepare(request, HttpResponse.build { body("Hello") })

        assertEquals("gzip", prepared.headers["Content-Encoding"])
        assertEquals("chunked", prepared.headers["Transfer-Encoding"])
        assertNull(prepared.headers["Content-Length"])
    }

    @Test
    fun `should not encode response body without gzip addon`() {
        val request = FallbackRequestMetadata(headers = HttpHeaders.of("Accept-Encoding" to "gzip"))

        val prepared = HttpResponsePreparer().prepare(request, HttpResponse.build { body("Hello") })

        assertNull(prepared.headers["Content-Encoding"])
        assertEquals("5", prepared.headers["Content-Length"])
    }

    @Test
    fun `should prefer identity when it has higher quality`() {
        val preparer =
            HttpResponsePreparer(
                contentNegotiator = ContentNegotiator(registry.responseBodyEncoders()),
            )
        val request =
            FallbackRequestMetadata(
                headers = HttpHeaders.of("Accept-Encoding" to "gzip;q=0.5, identity;q=1.0"),
            )

        val prepared = preparer.prepare(request, HttpResponse.build { body("Hello") })

        assertNull(prepared.headers["Content-Encoding"])
        assertEquals("5", prepared.headers["Content-Length"])
    }

    @Test
    fun `should encode and decode body with gzip codec`() {
        val codec = registry.responseBodyEncoders().getValue("gzip")
        val encoded = ByteArrayWritableChannel()
        codec.encodeTo(encoded, BodyProducerFactory.create(BodySpec.Text("Hello GZIP")))

        val decoded =
            registry.decodeRequestBody(
                HttpHeaders.of("Content-Encoding" to "gzip"),
                ByteArrayInputStream(encoded.toByteArray()),
            )

        assertEquals("Hello GZIP", decoded.readAllBytes().decodeToString())
    }

    @Test
    fun `request parser should expose decoded gzip body`() {
        val content = "Hello request GZIP"
        val compressed = gzip(content.encodeToByteArray())
        val requestHead =
            "POST / HTTP/1.1\r\n" +
                "Content-Encoding: gzip\r\n" +
                "Content-Length: ${compressed.size}\r\n\r\n"
        val channel = FixedReadableByteChannel(listOf(requestHead.encodeToByteArray(), compressed))
        val stream = ByteChannelReadStream(channel, ByteBuffer.allocate(1024).flip())
        val limits = HttpLimitsConfig()
        val parser = HttpRequestParser(HttpHeadersParser(limits), limits, registry::decodeRequestBody)

        val request = parser.parse(stream)

        assertEquals(content, request.bodyStream.readAllBytes().decodeToString())
    }

    @Test
    fun `should reject malformed gzip request body`() {
        val headers = HttpHeaders.of("Content-Encoding" to "gzip")

        assertThrows<BadRequestException> {
            registry.decodeRequestBody(headers, ByteArrayInputStream("not gzip".encodeToByteArray()))
        }
    }

    @Test
    fun `should reject gzip request body without gzip addon`() {
        val headers = HttpHeaders.of("Content-Encoding" to "gzip")
        val exception =
            assertThrows<HttpResponseException> {
                AddonRegistry().decodeRequestBody(headers, ByteArrayInputStream(gzip("Hello".encodeToByteArray())))
            }

        assertEquals(415, exception.httpResponse.status.code)
    }

    private fun gzip(bytes: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(bytes) }
        return output.toByteArray()
    }
}
