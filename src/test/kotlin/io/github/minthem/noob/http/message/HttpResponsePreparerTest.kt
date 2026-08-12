package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.codec.CodecRegistry
import io.github.minthem.noob.http.codec.GzipCodec
import io.github.minthem.noob.http.util.asCloseable
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HttpResponsePreparerTest {
    private val fixedClock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneId.of("UTC"))
    private val preparer = HttpResponsePreparer(fixedClock)

    private val dummyRequest = FallbackRequestMetadata()

    @Test
    fun `should set date header`() {
        val response = HttpResponse.build { body("hello") }
        val prepared = preparer.prepare(dummyRequest, response)

        assertEquals("Sun, 1 Jan 2023 00:00:00 GMT", prepared.headers["date"])
    }

    @Test
    fun `should set default content type for text body when not specified`() {
        val response = HttpResponse.build { body("Hello") }
        val prepared = preparer.prepare(dummyRequest, response)

        assertEquals("text/plain; charset=\"utf-8\"", prepared.headers["content-type"])
    }

    @Test
    fun `should not overwrite content type when specified explicitly`() {
        val response =
            HttpResponse.build {
                header("Content-Type", "text/html")
                body("Hello")
            }
        val prepared = preparer.prepare(dummyRequest, response)

        assertEquals("text/html", prepared.headers["content-type"])
    }

    @Test
    fun `should set content length automatically for text body`() {
        val response = HttpResponse.build { body("Hello") }
        val prepared = preparer.prepare(dummyRequest, response)

        assertEquals("5", prepared.headers["content-length"])
        assertNull(prepared.headers["transfer-encoding"])
        assertTrue(prepared.bodyWriter is FixedBodyWriter)
    }

    @Test
    fun `should set content length automatically for empty body`() {
        val response = HttpResponse.build { }
        val prepared = preparer.prepare(dummyRequest, response)

        assertEquals("0", prepared.headers["content-length"])
        assertNull(prepared.headers["transfer-encoding"])
        assertTrue(prepared.bodyWriter is FixedBodyWriter)
    }

    @Test
    fun `should overwrite explicit content length with calculated length for non chunked body`() {
        val response =
            HttpResponse.build {
                header("Content-Length", "999")
                body("Hello")
            }
        val prepared = preparer.prepare(dummyRequest, response)

        // The BodyProducer's actual length takes precedence over the explicit header
        assertEquals("5", prepared.headers["content-length"])
        assertNull(prepared.headers["transfer-encoding"])
    }

    @Test
    fun `should remove transfer encoding for non chunked body`() {
        val response =
            HttpResponse.build {
                header("Transfer-Encoding", "chunked")
                body("Hello")
            }
        val prepared = preparer.prepare(dummyRequest, response)

        assertEquals("5", prepared.headers["content-length"])
        assertNull(prepared.headers["transfer-encoding"])
    }

    @Test
    fun `should set transfer encoding chunked for chunk body`() {
        val response =
            HttpResponse.build {
                body(sequenceOf("hello", "world").asCloseable { })
            }
        val prepared = preparer.prepare(dummyRequest, response)

        assertEquals("chunked", prepared.headers["transfer-encoding"])
        assertNull(prepared.headers["content-length"])
        assertTrue(prepared.bodyWriter is ChunkedBodyWriter)
    }

    @Test
    fun `should remove explicit content length for chunked body`() {
        val response =
            HttpResponse.build {
                header("Content-Length", "999")
                body(sequenceOf("hello", "world").asCloseable { })
            }
        val prepared = preparer.prepare(dummyRequest, response)

        assertEquals("chunked", prepared.headers["transfer-encoding"])
        assertNull(prepared.headers["content-length"])
    }

    @Test
    fun `should encode response with accepted gzip codec`() {
        val preparer = HttpResponsePreparer(fixedClock, CodecRegistry(listOf(GzipCodec())))
        val request = FallbackRequestMetadata(headers = HttpHeaders.of("Accept-Encoding" to "gzip"))

        val prepared = preparer.prepare(request, HttpResponse.build { body("Hello") })

        assertEquals("gzip", prepared.headers["content-encoding"])
        assertEquals("chunked", prepared.headers["transfer-encoding"])
        assertNull(prepared.headers["content-length"])
        assertTrue(prepared.bodyWriter is ChunkedBodyWriter)
    }

    @Test
    fun `should prefer identity codec with higher quality`() {
        val preparer = HttpResponsePreparer(fixedClock, CodecRegistry(listOf(GzipCodec())))
        val request =
            FallbackRequestMetadata(
                headers = HttpHeaders.of("Accept-Encoding" to "gzip;q=0.5, identity;q=1.0"),
            )

        val prepared = preparer.prepare(request, HttpResponse.build { body("Hello") })

        assertNull(prepared.headers["content-encoding"])
        assertEquals("5", prepared.headers["content-length"])
        assertTrue(prepared.bodyWriter is FixedBodyWriter)
    }

    @Test
    fun `should fall back to identity for unsupported encoding`() {
        val request = FallbackRequestMetadata(headers = HttpHeaders.of("Accept-Encoding" to "br"))

        val prepared = preparer.prepare(request, HttpResponse.build { body("Hello") })

        assertNull(prepared.headers["content-encoding"])
        assertEquals("5", prepared.headers["content-length"])
    }
}
