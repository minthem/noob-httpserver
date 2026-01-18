package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.io.ByteChannelReader
import io.github.minthem.noobhttpserver.io.ByteReadStream
import java.io.ByteArrayOutputStream

internal interface BodySource {

    fun read(b: ByteArray, off: Int = 0, len: Int = b.size): Int
}


internal class FixedLengthBodySource(
    private val stream: ByteReadStream,
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
        val n = stream.read(b, off, canRead)

        if (n > 0) {
            remaining -= n
        }

        return if (n == 0 && remaining > 0) 0 else n
    }
}

internal class ChunkedBodySource(
    private val stream: ByteReadStream,
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
                    val line = readLine()

                    // chunk-data末尾のCRLFを読み飛ばす
                    if (line.isEmpty()) {
                        continue
                    }
                    val chunkSize = line.substringBefore(";").toLong(16)
                    if (chunkSize == 0L) {
                        state = State.READING_TRAILER
                    } else {
                        chunkRemain = chunkSize
                        state = State.READING_CHUNK_DATA
                    }
                }

                State.READING_CHUNK_DATA -> {
                    val canRead = minOf(chunkRemain, len.toLong()).toInt()
                    val n = stream.read(b, off, canRead)
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
                        readLine() // trailer
                        break
                    }
                    exhausted = true
                    return -1
                }
            }
        }
    }

    private fun readLine(): String {
        val buffer = ByteArrayOutputStream()
        while (true) {
            val b = stream.next()
            if (b == '\r'.code.toByte() && stream.peak() == '\n'.code) {
                stream.next()
                break
            }
            buffer.write(b.toInt())
        }

        return buffer.toString()
    }
}
