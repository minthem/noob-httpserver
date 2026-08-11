package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.testutil.ByteArrayWritableChannel
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyEncoderTest {
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
}
