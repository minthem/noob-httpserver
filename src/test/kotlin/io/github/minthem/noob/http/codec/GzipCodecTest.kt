package io.github.minthem.noob.http.codec

import io.github.minthem.noob.http.exception.BadRequestException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class GzipCodecTest {
    private val codec = GzipCodec()

    @Test
    fun `encodes and decodes gzip stream`() {
        val source = "Hello GZIP".encodeToByteArray()
        val encoded = ByteArrayOutputStream()
        codec.encode(encoded).use { it.write(source) }

        val actual = codec.decode(ByteArrayInputStream(encoded.toByteArray())).use { it.readAllBytes() }

        assertContentEquals(source, actual)
    }

    @Test
    fun `maps invalid gzip header to bad request`() {
        assertFailsWith<BadRequestException> {
            codec.decode(ByteArrayInputStream("not gzip".encodeToByteArray()))
        }
    }

    @Test
    fun `maps invalid gzip payload to bad request while reading`() {
        val truncated = byteArrayOf(0x1f, 0x8b.toByte(), 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val decoded = codec.decode(ByteArrayInputStream(truncated))

        assertFailsWith<BadRequestException> {
            decoded.readAllBytes()
        }
    }

    @Test
    fun `rejects non-positive buffer size`() {
        assertFailsWith<IllegalArgumentException> {
            GzipCodec(0)
        }
    }
}
