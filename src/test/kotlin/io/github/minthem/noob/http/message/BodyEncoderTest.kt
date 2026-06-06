package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.testutil.ByteArrayWritableChannel
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyEncoderTest {
    @Nested
    inner class BodyEncoderFactoryTest {
        @Test
        fun `should create default body encoder`() {
            val encoder = BodyEncoderFactory.create(BodyEncoding.IDENTITY)
            assertInstanceOf(DefaultBodyEncoder::class.java, encoder)
        }

        @Test
        fun `should create gzip body encoder`() {
            val encoder = BodyEncoderFactory.create(BodyEncoding.GZIP)
            assertInstanceOf(GzipBodyEncoder::class.java, encoder)
        }
    }

    @Nested
    inner class DefaultBodyEncoderTest {
        private val encoder = DefaultBodyEncoder()

        @Test
        fun `should correct preserves content length and content encoding`() {
            assertTrue(encoder.preservesContentLength)
            assertNull(encoder.contentEncoding)
        }

        @Test
        fun `should encode body producer to write dest`() {
            val content = "Hello, World!"
            val bodyProducer = BodyProducerFactory.create(BodySpec.Text(content))
            val writeDest = ByteArrayWritableChannel()

            encoder.encodeTo(writeDest, bodyProducer)

            val actual = String(writeDest.toByteArray())
            assertEquals(content, actual)
        }
    }

    @Nested
    inner class GzipBodyEncoderTest {
        private val encoder = GzipBodyEncoder()

        @Test
        fun `should correct preserves content length and content encoding`() {
            assertFalse(encoder.preservesContentLength)
            assertEquals(BodyEncoding.GZIP, encoder.contentEncoding)
        }

        @Test
        fun `should encode body producer to write dest with gzip when content exists`() {
            val content = "Hello, World! GZIP"
            val bodyProducer = BodyProducerFactory.create(BodySpec.Text(content))
            val writeDest = ByteArrayWritableChannel()

            encoder.encodeTo(writeDest, bodyProducer)

            val compressedBytes = writeDest.toByteArray()

            // GZIPInputStream を使って解凍し、元のテキストと一致するか検証
            val decompressed =
                GZIPInputStream(ByteArrayInputStream(compressedBytes))
                    .bufferedReader(Charsets.UTF_8)
                    .readText()

            assertEquals(content, decompressed)
        }

        @Test
        fun `should encode body producer to write dest with gzip when content is empty`() {
            val bodyProducer = BodyProducerFactory.create(BodySpec.Empty)
            val writeDest = ByteArrayWritableChannel()

            encoder.encodeTo(writeDest, bodyProducer)

            val compressedBytes = writeDest.toByteArray()
            assertTrue(compressedBytes.isNotEmpty(), "gzipのヘッダやフッター分は出力される")

            // 解凍した結果が空文字列になるか検証
            val decompressed =
                GZIPInputStream(ByteArrayInputStream(compressedBytes))
                    .bufferedReader(Charsets.UTF_8)
                    .readText()

            assertEquals("", decompressed)
        }

        @Test
        fun `should encode large content correctly`() {
            // 大きめのデータ (約100KB)
            val content = "1234567890".repeat(10000)
            val bodyProducer = BodyProducerFactory.create(BodySpec.Text(content))

            // バッファサイズを意図的に小さくして複数回書き込みが発生するようにする
            val writeDest = ByteArrayWritableChannel(bufferSize = 1024)

            encoder.encodeTo(writeDest, bodyProducer)

            val compressedBytes = writeDest.toByteArray()

            val decompressed =
                GZIPInputStream(ByteArrayInputStream(compressedBytes))
                    .bufferedReader(Charsets.UTF_8)
                    .readText()

            assertEquals(content, decompressed)
        }
    }
}
