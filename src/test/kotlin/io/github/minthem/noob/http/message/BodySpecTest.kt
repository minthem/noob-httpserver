package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.util.asCloseable
import org.junit.jupiter.api.Nested
import java.nio.charset.Charset
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodySpecTest {
    @Nested
    inner class EmptyBodyTest {
        @Test
        fun `should return empty body`() {
            val empty = BodySpec.Empty
            assertNull(empty.contentLength)
            assertNull(empty.defaultContentType)
        }
    }

    @Nested
    inner class TextBodyTest {
        @Test
        fun `should return text body`() {
            val text = BodySpec.Text("HELLO WORLD")
            assertEquals(11L, text.contentLength)
            assertTrue(MediaType.TEXT_PLAIN.isCompatibleWith(text.defaultContentType))
            assertEquals(Charsets.UTF_8, text.defaultContentType.charset)
        }

        @Test
        fun `should return text body with custom charset`() {
            val text = BodySpec.Text("HELLO WORLD", Charset.forName("UTF-16"))
            assertEquals(24L, text.contentLength)
            assertTrue(MediaType.TEXT_PLAIN.isCompatibleWith(text.defaultContentType))
            assertEquals(Charset.forName("UTF-16"), text.defaultContentType.charset)
        }
    }

    @Nested
    inner class BinaryBodyTest {
        @Test
        fun `should return binary body`() {
            val binary = BodySpec.Binary(byteArrayOf(0x01, 0x02, 0x03))
            assertEquals(3L, binary.contentLength)
            assertEquals(MediaType.OCTET_STREAM, binary.defaultContentType)
        }
    }

    @Nested
    inner class FileBodyTest {
        @Test
        fun `should return text file body`() {
            val tempFile = Files.createTempFile("test", ".txt")
            tempFile.writeText("HELLO WORLD")

            val file = BodySpec.File(tempFile)
            assertEquals(11L, file.contentLength)
            assertTrue(MediaType.TEXT_PLAIN.isCompatibleWith(file.defaultContentType))
            assertEquals(Charsets.UTF_8, file.defaultContentType.charset)

            tempFile.deleteIfExists()
        }

        @Test
        fun `should return html file body`() {
            val tempFile = Files.createTempFile("test", ".html")
            tempFile.writeText("<html><body>HELLO WORLD</body></html>")

            val file = BodySpec.File(tempFile)
            assertEquals(37L, file.contentLength)
            assertTrue(MediaType.TEXT_HTML.isCompatibleWith(file.defaultContentType))
            assertEquals(Charsets.UTF_8, file.defaultContentType.charset)

            tempFile.deleteIfExists()
        }

        @Test
        fun `should return text file body with UTF-16 charset`() {
            val tempFile = Files.createTempFile("test", ".txt")
            tempFile.writeText("HELLO WORLD", charset = Charsets.UTF_16)

            val file = BodySpec.File(tempFile, Charsets.UTF_16)
            assertEquals(24L, file.contentLength)
            assertTrue(MediaType.TEXT_PLAIN.isCompatibleWith(file.defaultContentType))
            assertEquals(Charsets.UTF_16, file.defaultContentType.charset)

            tempFile.deleteIfExists()
        }
    }

    @Nested
    inner class StreamingBodySpecTest {
        @Test
        fun `should return streaming body with custom content length and default content type`() {
            val source =
                sequenceOf(
                    "Hello".toByteArray(),
                    "World".toByteArray(),
                ).asCloseable { }
            val streamingBody = BodySpec.Streaming(source)

            assertNull(streamingBody.contentLength)
            assertEquals(MediaType.OCTET_STREAM, streamingBody.defaultContentType)
        }
    }
}
