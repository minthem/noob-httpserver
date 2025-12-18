package io.github.minthem.noobhttpserver.http.request

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

    companion object {
        fun fromStrings(strings: List<String>): ReadableByteMock {
            val bytes = strings.map { it.toByteArray() }
            return ReadableByteMock(bytes)
        }
    }
}