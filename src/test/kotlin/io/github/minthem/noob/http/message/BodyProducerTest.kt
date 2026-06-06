package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.testutil.ByteArrayWritableChannel
import io.github.minthem.noob.http.util.asCloseable
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.charset.Charset
import java.nio.file.Paths
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.fileSize
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BodyProducerTest {
    @Nested
    inner class BodyProducerFactoryTest {
        @Test
        fun `should create empty body producer`() {
            val spec = BodySpec.Empty
            val producer = BodyProducerFactory.create(spec)
            assertInstanceOf(EmptyBodyProducer::class.java, producer)
        }

        @Test
        fun `should create text body producer`() {
            val spec = BodySpec.Text("Hello, World!")
            val producer = BodyProducerFactory.create(spec)
            assertInstanceOf(TextBodyProducer::class.java, producer)
        }

        @Test
        fun `should create binary body producer`() {
            val spec = BodySpec.Binary(byteArrayOf(0x01, 0x02, 0x03))
            val producer = BodyProducerFactory.create(spec)
            assertInstanceOf(BinaryBodyProducer::class.java, producer)
        }

        @Test
        fun `should create file body producer`() {
            val path = createTempFile(prefix = "test", suffix = ".txt")

            val spec = BodySpec.File(path)
            val producer = BodyProducerFactory.create(spec)
            assertInstanceOf(FileBodyProducer::class.java, producer)

            path.deleteIfExists()
        }

        @Test
        fun `should throw exception when file does not exist`() {
            val path = Paths.get("non-existent-file.txt")
            val spec = BodySpec.File(path)

            assertThrows<NoSuchFileException> {
                BodyProducerFactory.create(spec)
            }
        }

        @Test
        fun `should throw exception when file is not readable`() {
            val path = createTempFile(prefix = "test", suffix = ".txt")
            path.toFile().setReadable(false)

            val spec = BodySpec.File(path)
            assertThrows<AccessDeniedException> {
                BodyProducerFactory.create(spec)
            }
        }

        @Test
        fun `should create streaming body producer`() {
            val streaming =
                sequenceOf(
                    "Hello".toByteArray(),
                    "World".toByteArray(),
                ).asCloseable { }
            val spec = BodySpec.Streaming(streaming)
            val producer = BodyProducerFactory.create(spec)
            assertInstanceOf(StreamingBodyProducer::class.java, producer)
        }
    }

    @Nested
    inner class FileBodyProducerTest {
        @ParameterizedTest(name = "should default content type to {0}")
        @CsvSource(
            ".txt, text/plain, UTF-8",
            ".html, text/html, Shift_JIS",
        )
        fun `should return correct default content type and content length`(
            ext: String,
            expectedType: String,
            expectedCharset: String,
        ) {
            val path = createTempFile(prefix = "test", suffix = ext)
            path.writeText("Hello, World!")
            val charset = Charset.forName(expectedCharset)

            val producer = BodyProducerFactory.create(BodySpec.File(path, charset))
            assertEquals(path.fileSize(), producer.contentLength)
            val actualContentType = producer.defaultContentType
            assertNotNull(actualContentType)
            assertEquals(
                MediaType.parse(expectedType).withCharset(charset),
                actualContentType,
            )
        }

        @Test
        fun `should write file to writable channel`() {
            val path = createTempFile(prefix = "test", suffix = ".txt")
            path.writeText("Hello, World!")
            val producer = BodyProducerFactory.create(BodySpec.File(path))

            val writeChannel =
                ByteArrayWritableChannel(
                    bufferSize = 1024,
                )
            producer.writeTo(writeChannel)

            val actual = String(writeChannel.toByteArray())
            assertEquals("Hello, World!", actual)
        }

        @Test
        fun `should write file when large content`() {
            val path = createTempFile(prefix = "test", suffix = ".txt")
            val content = "1234567890".repeat(100)
            path.writeText(content)
            val producer = BodyProducerFactory.create(BodySpec.File(path))

            val writeChannel =
                ByteArrayWritableChannel(
                    bufferSize = 499,
                )
            producer.writeTo(writeChannel)

            val actual = String(writeChannel.toByteArray())
            assertEquals(content, actual)
        }
    }

    @Nested
    inner class EmptyBodyProducerTest {
        @Test
        fun `should return correct default content type and content length`() {
            val producer = BodyProducerFactory.create(BodySpec.Empty)

            assertEquals(0L, producer.contentLength)
            assertEquals(null, producer.defaultContentType)
        }

        @Test
        fun `should not write anything to writable channel`() {
            val producer = BodyProducerFactory.create(BodySpec.Empty)
            val writeChannel = ByteArrayWritableChannel()

            producer.writeTo(writeChannel)

            assertEquals(0, writeChannel.toByteArray().size)
        }
    }

    @Nested
    inner class BinaryBodyProducerTest {
        @Test
        fun `should return correct default content type and content length`() {
            val bytes = byteArrayOf(0x01, 0x02, 0x03)
            val producer = BodyProducerFactory.create(BodySpec.Binary(bytes))

            assertEquals(3L, producer.contentLength)
            assertEquals(MediaType.parse("application/octet-stream"), producer.defaultContentType)
        }

        @Test
        fun `should write binary data to writable channel`() {
            val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
            val producer = BodyProducerFactory.create(BodySpec.Binary(bytes))

            val writeChannel = ByteArrayWritableChannel(bufferSize = 2)
            producer.writeTo(writeChannel)

            val actual = writeChannel.toByteArray()
            assertEquals(bytes.toList(), actual.toList())
        }
    }

    @Nested
    inner class TextBodyProducerTest {
        @Test
        fun `should return correct default content type and content length`() {
            val text = "Hello, World!"
            val producer = BodyProducerFactory.create(BodySpec.Text(text))

            val expectedBytes = text.toByteArray(Charsets.UTF_8)
            assertEquals(expectedBytes.size.toLong(), producer.contentLength)
            assertEquals(MediaType.parse("text/plain").withCharset(Charsets.UTF_8), producer.defaultContentType)
        }

        @Test
        fun `should write text to writable channel`() {
            val text = "Hello, World!"
            val producer = BodyProducerFactory.create(BodySpec.Text(text))

            val writeChannel = ByteArrayWritableChannel(bufferSize = 5)
            producer.writeTo(writeChannel)

            val actual = String(writeChannel.toByteArray(), Charsets.UTF_8)
            assertEquals(text, actual)
        }
    }

    @Nested
    inner class StreamingBodyProducerTest {
        @Test
        fun `should return correct default content type and null content length`() {
            val streaming = sequenceOf("Test".toByteArray()).asCloseable { }
            val producer = BodyProducerFactory.create(BodySpec.Streaming(streaming))

            assertEquals(null, producer.contentLength)
            assertEquals(MediaType.OCTET_STREAM, producer.defaultContentType)
        }

        @Test
        fun `should write streamed data to writable channel`() {
            val streaming =
                sequenceOf(
                    "Hello, ".toByteArray(),
                    "World!".toByteArray(),
                ).asCloseable { }
            val producer = BodyProducerFactory.create(BodySpec.Streaming(streaming))

            val writeChannel = ByteArrayWritableChannel(bufferSize = 4)
            producer.writeTo(writeChannel)

            val actual = String(writeChannel.toByteArray())
            assertEquals("Hello, World!", actual)
        }
    }
}
