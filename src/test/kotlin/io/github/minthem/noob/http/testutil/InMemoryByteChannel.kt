package io.github.minthem.noob.http.testutil

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel

class InMemoryByteChannel(
    inputChunks: List<ByteArray>
) : ByteChannel {

    private var readableChunks = inputChunks.toList()
    private val written = ByteArrayOutputStream()
    private var open = true

    override fun read(dst: ByteBuffer): Int {
        val next = readableChunks.firstOrNull() ?: return -1
        readableChunks = readableChunks.drop(1)
        dst.put(next)
        return next.size
    }

    override fun write(src: ByteBuffer): Int {
        val size = src.remaining()
        val bytes = ByteArray(size)
        src.get(bytes)
        written.write(bytes)
        return size
    }

    override fun isOpen(): Boolean = open

    override fun close() {
        open = false
    }

    fun writtenText(): String = written.toByteArray().toString(Charsets.UTF_8)

    companion object {
        fun fromStrings(strings: List<String>): InMemoryByteChannel {
            return InMemoryByteChannel(strings.map { it.toByteArray() })
        }
    }
}