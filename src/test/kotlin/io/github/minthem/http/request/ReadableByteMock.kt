package io.github.minthem.http.request

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

class ReadableByteMock(private var bytes: List<ByteArray>) : ReadableByteChannel {

    override fun read(p0: ByteBuffer): Int {
        val b = bytes.first()
        bytes = bytes.drop(1)

        p0.put(b)
        return b.size
    }

    override fun isOpen(): Boolean = true

    override fun close() {}
}