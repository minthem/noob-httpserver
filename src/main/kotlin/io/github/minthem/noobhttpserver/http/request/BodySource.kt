package io.github.minthem.noobhttpserver.http.request

import io.github.minthem.noobhttpserver.http.socket.SocketReadBuffer

internal interface BodySource {

    fun read(b: ByteArray, off: Int = 0, len: Int = b.size): Int
}


internal class FixedLengthBodySource(
    private val sockerBuffer: SocketReadBuffer,
    private val length: Long
) : BodySource {

    init {
        require(length >= 0) { "length must be non-negative: $length" }
    }

    private var remaining = length

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len > b.size - off) {
            throw IndexOutOfBoundsException("offset: $off, length: $len, array size: ${b.size}")
        }

        if (remaining == 0L) {
            return -1
        }
        if (len == 0) {
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

internal class ChunkedBodySource(
    private val sockerBuffer: SocketReadBuffer
) : BodySource {

    private enum class State {
        READING_CHUNK_SIZE, READING_CHUNK_DATA, READING_TRAILER
    }

    private var state = State.READING_CHUNK_SIZE
    private var chunkRemain = 0L
    private var exhausted = false

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (off < 0 || len < 0 || len > b.size - off) {
            throw IndexOutOfBoundsException("offset: $off, length: $len, array size: ${b.size}")
        }

        if (exhausted) {
            return -1
        }

        if (len == 0) {
            return 0
        }

        while (true) {
            when (state) {
                State.READING_CHUNK_SIZE -> {
                    val line = sockerBuffer.tryReadLine() ?: return 0

                    // chunk-data末尾のCRLFを読み飛ばす
                    if (line.isEmpty()) {
                        continue
                    }
                    val chunkSize = line.substringBefore(";").toLong(16)
                    if (chunkSize == 0L) {
                        state = State.READING_TRAILER
                    } else {
                        state = State.READING_CHUNK_DATA
                    }
                }

                State.READING_CHUNK_DATA -> {
                    val canRead = minOf(chunkRemain, len.toLong()).toInt()
                    val n = sockerBuffer.read(b, off, canRead)
                    if (n > 0) {
                        chunkRemain -= n
                        if (chunkRemain == 0L) {
                            state = State.READING_CHUNK_SIZE
                        }
                    }
                    return n
                }

                State.READING_TRAILER -> {
                    while (true) {
                        @Suppress("unused")
                        val trailer = sockerBuffer.tryReadLine() ?: return 0
                        break
                    }
                    exhausted = true
                    return -1
                }
            }
        }
    }
}
