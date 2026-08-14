package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.config.HttpLimitsConfig
import io.github.minthem.noob.http.exception.BodySizeExceededException
import io.github.minthem.noob.http.exception.ChunkTooLargeException
import io.github.minthem.noob.http.io.ByteReadStream
import java.io.ByteArrayOutputStream

internal interface BodySource {
    fun read(
        b: ByteArray,
        off: Int = 0,
        len: Int = b.size,
    ): Int
}

internal class FixedLengthBodySource(
    private val stream: ByteReadStream,
    private val length: Long,
) : BodySource {
    init {
        require(length >= 0) { "length must be non-negative: $length" }
    }

    private var remaining = length

    override fun read(
        b: ByteArray,
        off: Int,
        len: Int,
    ): Int {
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
    private val limitsConfig: HttpLimitsConfig,
) : BodySource {
    init {
        require(limitsConfig.maxChunkSizeBytes > 0) { "Chunk size limit must be greater than 0" }
    }

    private enum class State {
        READING_CHUNK_SIZE,
        READING_CHUNK_DATA,
        READING_TRAILER,
    }

    private var state = State.READING_CHUNK_SIZE
    private var chunkRemain = 0L
    private var exhausted = false
    private var totalRead = 0L

    override fun read(
        b: ByteArray,
        off: Int,
        len: Int,
    ): Int {
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
                    if (limitsConfig.maxChunkSizeBytes < chunkSize) {
                        exhausted = true
                        throw ChunkTooLargeException(
                            chunkSize = chunkSize,
                            maxChunkSizeBytes = limitsConfig.maxChunkSizeBytes,
                        )
                    }
                    if (chunkSize == 0L) {
                        state = State.READING_TRAILER
                    } else {
                        chunkRemain = chunkSize
                        state = State.READING_CHUNK_DATA
                    }
                }

                State.READING_CHUNK_DATA -> {
                    if (chunkRemain == 0L) {
                        state = State.READING_CHUNK_SIZE
                        continue
                    }
                    val canRead = minOf(chunkRemain, len.toLong()).toInt()
                    val n = stream.read(b, off, canRead)
                    if (n > 0) {
                        chunkRemain -= n
                        totalRead += n

                        if (chunkRemain == 0L) {
                            state = State.READING_CHUNK_SIZE
                        }

                        if (limitsConfig.maxRequestBodyBytes < totalRead) {
                            exhausted = true
                            throw BodySizeExceededException(
                                actualBytesRead = totalRead,
                                maxBodySizeBytes = limitsConfig.maxRequestBodyBytes,
                            )
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
