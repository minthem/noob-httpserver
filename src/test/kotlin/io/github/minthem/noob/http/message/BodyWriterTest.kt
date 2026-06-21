package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.testutil.ByteArrayWritableChannel
import io.github.minthem.noob.http.util.asCloseable
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BodyWriterTest {
    @Nested
    inner class FixedBodyWriterTest {
        @Test
        fun `should write fixed body`() {
            val producer = BodyProducerFactory.create(BodySpec.Text("Hello, World!"))
            val encoder = BodyEncoderFactory.create(BodyEncoding.IDENTITY)

            val writeChannel = ByteArrayWritableChannel()

            val writer = FixedBodyWriter(producer, encoder)
            writer.write(writeChannel)

            val actual = String(writeChannel.toByteArray())
            assertEquals("Hello, World!", actual)
        }

        @Test
        fun `should write empty body`() {
            val producer = BodyProducerFactory.create(BodySpec.Empty)
            val encoder = BodyEncoderFactory.create(BodyEncoding.IDENTITY)

            val writeChannel = ByteArrayWritableChannel()

            val writer = FixedBodyWriter(producer, encoder)
            writer.write(writeChannel)

            val actual = String(writeChannel.toByteArray())
            assertEquals("", actual)
        }

        @Test
        fun `should throw exception when preserves content length is false`() {
            val producer = BodyProducerFactory.create(BodySpec.Text("Hello, World!"))
            val encoder = BodyEncoderFactory.create(BodyEncoding.GZIP)

            assertFailsWith<IllegalArgumentException> {
                FixedBodyWriter(producer, encoder)
            }
        }

        @Test
        fun `should throw exception when content length is not set`() {
            val stream = sequenceOf("Hello".toByteArray(), "World".toByteArray()).asCloseable { }
            val producer = BodyProducerFactory.create(BodySpec.Streaming(stream))
            val encoder = BodyEncoderFactory.create(BodyEncoding.IDENTITY)

            assertFailsWith<IllegalArgumentException> {
                FixedBodyWriter(producer, encoder)
            }
        }
    }

    @Nested
    inner class ChunkedBodyWriterTest {
        @Test
        fun `should write chunked body`() {
            val body = (1..20).map { "1".repeat(it) }
            val stream =
                sequence {
                    for (chunk in body) {
                        yield(chunk.toByteArray())
                    }
                }.asCloseable { }
            val producer = BodyProducerFactory.create(BodySpec.Streaming(stream))
            val encoder = BodyEncoderFactory.create(BodyEncoding.IDENTITY)

            val writer = ChunkedBodyWriter(producer, encoder)
            val writeChannel = ByteArrayWritableChannel()
            writer.write(writeChannel)

            val expected =
                body.joinToString("") {
                    val length = it.length.toString(16)
                    "$length\r\n$it\r\n"
                } + "0\r\n\r\n"

            val actual = String(writeChannel.toByteArray())
            assertEquals(expected, actual)
        }

        @Test
        fun `should write empty body`() {
            val producer = BodyProducerFactory.create(BodySpec.Empty)
            val encoder = BodyEncoderFactory.create(BodyEncoding.IDENTITY)
            val writer = ChunkedBodyWriter(producer, encoder)
            val writeChannel = ByteArrayWritableChannel()
            writer.write(writeChannel)

            val expected = "0\r\n\r\n"

            val actual = String(writeChannel.toByteArray())
            assertEquals(expected, actual)
        }

        @Test
        fun `should write fixed body`() {
            val producer = BodyProducerFactory.create(BodySpec.Text("Hello, World!"))
            val encoder = BodyEncoderFactory.create(BodyEncoding.IDENTITY)
            val writer = ChunkedBodyWriter(producer, encoder)
            val writeChannel = ByteArrayWritableChannel()
            writer.write(writeChannel)

            val expected = "d\r\nHello, World!\r\n0\r\n\r\n"

            val actual = String(writeChannel.toByteArray())
            assertEquals(expected, actual)
        }
    }
}
