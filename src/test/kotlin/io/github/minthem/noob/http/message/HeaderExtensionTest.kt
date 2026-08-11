package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.multipart.ContentDisposition
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class HeaderExtensionTest {
    @Nested
    inner class ContentTypeTest {
        @Test
        fun `should return content type`() {
            val headers = HttpHeaders.of("Content-Type" to "application/json")
            val expected = MediaType.parse("application/json")
            assertEquals(expected, headers.contentType)
        }

        @Test
        fun `should return null if content type is not present`() {
            val headers = HttpHeaders.of()
            val actual = headers.contentType
            assertEquals(null, actual)
        }

        @Test
        fun `should return first content type when multiple content types are present`() {
            val headers = HttpHeaders.of("Content-Type" to "application/json", "Content-Type" to "text/plain")
            val expected = MediaType.parse("application/json")
            val actual = headers.contentType
            assertEquals(expected, actual)
        }

        @Test
        fun `should add content-type when set content type`() {
            val headers = MutableHttpHeaders()
            headers.contentType = MediaType.parse("application/json")
            assertEquals("application/json", headers["Content-Type"])
        }

        @Test
        fun `should remove content type when content type is set to null`() {
            val headers = MutableHttpHeaders()
            headers.contentType = MediaType.parse("application/json")
            headers.contentType = null
            assertEquals(null, headers["Content-Type"])
        }

        @Test
        fun `should replace content type when content type is set to a different value`() {
            val headers = MutableHttpHeaders()
            headers.contentType = MediaType.parse("application/json")
            headers.contentType = MediaType.parse("text/plain")
            assertEquals("text/plain", headers["Content-Type"])
        }
    }

    @Nested
    inner class ContentLengthTest {
        @Test
        fun `should return content length`() {
            val headers = MutableHttpHeaders()
            headers.contentLength = 100
            assertEquals("100", headers["Content-Length"])
        }

        @Test
        fun `should remove content length when content length is set to null`() {
            val headers = MutableHttpHeaders()
            headers.contentLength = 100
            headers.contentLength = null
            assertEquals(null, headers["Content-Length"])
        }

        @Test
        fun `should replace content length when content length is set to a different value`() {
            val headers = MutableHttpHeaders()
            headers.contentLength = 100
            headers.contentLength = 200
            assertEquals("200", headers["Content-Length"])
        }

        @Test
        fun `should throw exception when content length is negative`() {
            val headers = MutableHttpHeaders()
            val exception =
                assertFailsWith<IllegalArgumentException> {
                    headers.contentLength = -1
                }
            assertEquals("Content-Length must be greater than or equal to 0", exception.message)
        }
    }

    @Nested
    inner class ContentDispositionTest {
        @Test
        fun `should return content disposition`() {
            val headers = HttpHeaders.of("Content-Disposition" to "attachment; filename=test.txt")
            val contentDisposition = headers.contentDisposition
            assertEquals("attachment; filename=\"test.txt\"", contentDisposition?.toString())
        }

        @Test
        fun `should return null if content disposition is not present`() {
            val headers = HttpHeaders.of()
            val contentDisposition = headers.contentDisposition
            assertEquals(null, contentDisposition)
        }

        @Test
        fun `should return first content disposition when multiple content dispositions are present`() {
            val headers =
                HttpHeaders.of(
                    "Content-Disposition" to "attachment; filename=test.txt",
                    "Content-Disposition" to "inline",
                )
            val contentDisposition = headers.contentDisposition
            assertEquals("attachment; filename=\"test.txt\"", contentDisposition?.toString())
        }

        @Test
        fun `should add content-disposition when set content disposition`() {
            val headers = MutableHttpHeaders()
            headers.contentDisposition = ContentDisposition.parse("attachment; filename=test.txt")
            assertEquals("attachment; filename=\"test.txt\"", headers["Content-Disposition"])
        }

        @Test
        fun `should remove content disposition when content disposition is set to null`() {
            val headers = MutableHttpHeaders()
            headers.contentDisposition = ContentDisposition.parse("attachment; filename=test.txt")
            headers.contentDisposition = null
            assertEquals(null, headers["Content-Disposition"])
        }

        @Test
        fun `should replace content disposition when content disposition is set to a different value`() {
            val headers = MutableHttpHeaders()
            headers.contentDisposition = ContentDisposition.parse("attachment; filename=test.txt")
            headers.contentDisposition = ContentDisposition.parse("inline")
            assertEquals("inline", headers["Content-Disposition"])
        }
    }

    @Nested
    inner class AcceptEncodingTest {
        @Test
        fun `should return accept encoding`() {
            val headers = HttpHeaders.of("Accept-Encoding" to "gzip")
            val expected = listOf(BodyEncoding.GZIP)
            val actual = headers.acceptEncoding
            assertEquals(expected, actual)
        }

        @Test
        fun `should return empty list if accept encoding is not present`() {
            val headers = HttpHeaders.of()
            val actual = headers.acceptEncoding
            assertNull(actual)
        }

        @Test
        fun `should return multiple accept encodings`() {
            val headers = HttpHeaders.of("Accept-Encoding" to "gzip, deflate", "Accept-Encoding" to "identity")
            val expected = listOf(BodyEncoding.GZIP, BodyEncoding.parse("deflate"), BodyEncoding.IDENTITY)
            val actual = headers.acceptEncoding
            assertEquals(expected, actual)
        }

        @Test
        fun `should add accept encoding when set accept encoding`() {
            val headers = MutableHttpHeaders()
            headers.acceptEncoding = listOf(BodyEncoding.GZIP)
            assertEquals("gzip", headers["Accept-Encoding"])
        }

        @Test
        fun `should remove accept encoding when accept encoding is set to empty list`() {
            val headers = MutableHttpHeaders()
            headers.acceptEncoding = listOf(BodyEncoding.GZIP)
            headers.acceptEncoding = emptyList()
            assertNull(headers["Accept-Encoding"])
        }

        @Test
        fun `should remove accept encoding when accept encoding is set to null`() {
            val headers = MutableHttpHeaders()
            headers.acceptEncoding = listOf(BodyEncoding.GZIP)
            headers.acceptEncoding = null
            assertNull(headers["Accept-Encoding"])
        }

        @Test
        fun `should replace accept encoding when accept encoding is set to a different value`() {
            val headers = MutableHttpHeaders()
            headers.acceptEncoding = listOf(BodyEncoding.GZIP)
            headers.acceptEncoding = listOf(BodyEncoding.parse("deflate"))
            assertEquals("deflate", headers["Accept-Encoding"])
        }

        @Test
        fun `should set multiple accept encodings`() {
            val headers = MutableHttpHeaders()
            headers.acceptEncoding = listOf(BodyEncoding.GZIP, BodyEncoding.parse("deflate"))
            assertEquals("gzip,deflate", headers["Accept-Encoding"])
        }
    }

    @Nested
    inner class ContentEncodingTest {
        @Test
        fun `should return content encoding`() {
            val headers = HttpHeaders.of("Content-Encoding" to "gzip")
            val expected = listOf(BodyEncoding.GZIP)
            val actual = headers.contentEncoding
            assertEquals(expected, actual)
        }

        @Test
        fun `should return empty list if content encoding is not present`() {
            val headers = HttpHeaders.of()
            val actual = headers.contentEncoding
            assertNull(actual)
        }

        @Test
        fun `should return multiple content encodings`() {
            val headers = HttpHeaders.of("Content-Encoding" to "gzip, deflate", "Content-Encoding" to "identity")
            val expected = listOf(BodyEncoding.GZIP, BodyEncoding.parse("deflate"), BodyEncoding.IDENTITY)
            val actual = headers.contentEncoding
            assertEquals(expected, actual)
        }

        @Test
        fun `should add content encoding when set content encoding`() {
            val headers = MutableHttpHeaders()
            headers.contentEncoding = listOf(BodyEncoding.GZIP)
            assertEquals("gzip", headers["Content-Encoding"])
        }

        @Test
        fun `should remove content encoding when content encoding is set to empty list`() {
            val headers = MutableHttpHeaders()
            headers.acceptEncoding = listOf(BodyEncoding.GZIP)
            headers.acceptEncoding = emptyList()
            assertNull(headers["Content-Encoding"])
        }

        @Test
        fun `should remove content encoding when content encoding is set to null`() {
            val headers = MutableHttpHeaders()
            headers.acceptEncoding = listOf(BodyEncoding.GZIP)
            headers.acceptEncoding = null
            assertNull(headers["Content-Encoding"])
        }

        @Test
        fun `should replace content encoding when content encoding is set to a different value`() {
            val headers = MutableHttpHeaders()
            headers.contentEncoding = listOf(BodyEncoding.GZIP)
            headers.contentEncoding = listOf(BodyEncoding.parse("deflate"))
            assertEquals("deflate", headers["Content-Encoding"])
        }

        @Test
        fun `should set multiple accept encodings`() {
            val headers = MutableHttpHeaders()
            headers.contentEncoding = listOf(BodyEncoding.GZIP, BodyEncoding.parse("deflate"))
            assertEquals("gzip,deflate", headers["Content-Encoding"])
        }
    }
}
