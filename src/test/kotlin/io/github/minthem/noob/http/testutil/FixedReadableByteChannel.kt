package io.github.minthem.noob.http.testutil

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

class FixedReadableByteChannel(private var bytes: List<ByteArray>) : ReadableByteChannel {

    override fun read(p0: ByteBuffer): Int {
        val b = bytes.firstOrNull() ?: return -1
        bytes = bytes.drop(1)

        p0.put(b)
        return b.size
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