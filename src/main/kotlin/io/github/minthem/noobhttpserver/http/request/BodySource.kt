package io.github.minthem.noobhttpserver.http.request

import io.github.minthem.noobhttpserver.http.socket.SocketReadBuffer

internal interface BodySource {

    fun read(b: ByteArray, off: Int = 0, len: Int = b.size): Int
}


internal class FixedLengthBodySource(
    private val sockerBuffer: SocketReadBuffer,
    private val length: Long
): BodySource {

    init {
        require(length >= 0) { "length must be non-negative: $length" }
    }

    private var remaining = length

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len > b.size - off) {
            throw IndexOutOfBoundsException("offset: $off, length: $len, array size: ${b.size}")
        }

        if(remaining == 0L) {
            return -1
        }
        if(len == 0) {
            return 0
        }

        val canRead = minOf(remaining, len.toLong()).toInt()
        val n = sockerBuffer.read(b, off, canRead)

        if (n > 0) {
            remaining -= n
        }

        return if (n == 0 && remaining > 0) 0 else n
    }
}