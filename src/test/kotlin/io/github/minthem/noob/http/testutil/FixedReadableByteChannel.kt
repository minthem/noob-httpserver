package io.github.minthem.noob.http.testutil

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

class FixedReadableByteChannel(
    private var bytes: List<ByteArray>,
) : ReadableByteChannel {
    override fun read(p0: ByteBuffer): Int {
        val firstBytes = bytes.firstOrNull() ?: return -1
        bytes = bytes.drop(1)

        val putBytes = if (firstBytes.size > p0.remaining()) {
            val bytesToRead = p0.remaining()
            bytes = listOf(firstBytes.sliceArray(bytesToRead until firstBytes.size)) + bytes
            firstBytes.sliceArray(0 until bytesToRead)
        } else {
            firstBytes
        }

        p0.put(putBytes)
        return firstBytes.size
    }

    override fun isOpen(): Boolean = true

    override fun close() {}

    companion object {
        fun fromStrings(strings: List<String>): FixedReadableByteChannel {
            val bytes = strings.map { it.toByteArray() }
            return FixedReadableByteChannel(bytes)
        }
    }
}
