package io.github.minthem.noob.http.codec

import java.io.InputStream
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class CodecRegistryTest {
    @Test
    fun `returns registered codec as encoder and decoder`() {
        val codec = TestCodec("test")
        val registry = CodecRegistry(listOf(codec))

        assertEquals(codec, registry.getEncoder("test"))
        assertEquals(codec, registry.getDecoder("test"))
    }

    @Test
    fun `returns null for unregistered codec`() {
        val registry = CodecRegistry()

        assertNull(registry.getEncoder("missing"))
        assertNull(registry.getDecoder("missing"))
    }

    @Test
    fun `registers identity codec by default`() {
        val registry = CodecRegistry()

        assertIs<NativeCodec>(registry.getEncoder("identity"))
        assertIs<NativeCodec>(registry.getDecoder("identity"))
    }

    @Test
    fun `rejects duplicate codec ids`() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                CodecRegistry(listOf(TestCodec("test"), TestCodec("test")))
            }

        assertEquals("Codec is already registered: test", exception.message)
    }

    @Test
    fun `rejects replacement of identity codec`() {
        assertFailsWith<IllegalArgumentException> {
            CodecRegistry(listOf(NativeCodec()))
        }
    }

    @Test
    fun `rejects invalid codec id`() {
        assertFailsWith<IllegalArgumentException> {
            CodecRegistry(listOf(TestCodec("invalid codec")))
        }
    }

    @Test
    fun `rejects non-lowercase codec id`() {
        assertFailsWith<IllegalArgumentException> {
            CodecRegistry(listOf(TestCodec("GZIP")))
        }
    }

    private class TestCodec(
        override val id: String,
    ) : StreamCodec {
        override fun encode(output: OutputStream): OutputStream = output

        override fun decode(input: InputStream): InputStream = input
    }
}
