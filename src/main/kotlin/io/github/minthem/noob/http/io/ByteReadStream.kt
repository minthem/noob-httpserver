package io.github.minthem.noob.http.io

import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

internal interface ByteReadStream {
    /**
     * Retrieves the next byte from the stream. If the buffer is empty, an internal refill
     * operation is triggered to fetch additional data from the underlying source.
     *
     * @return The next byte from the stream.
     * @throws EOFException if the end of the stream is reached unexpectedly during refill.
     */
    fun next(): Byte

    /**
     * Retrieves the next byte from the stream without advancing the buffer's position.
     * If the buffer is empty, an attempt is made to refill it with data from the underlying source.
     * Returns -1 if the end of the stream is encountered during the refill.
     *
     * @return The next byte as an integer in the range 0 to 255, or -1 if the end of the stream is reached.
     */
    fun peak(): Int

    /**
     * Reads a sequence of bytes from the stream into the specified byte array.
     *
     * @param dst The destination byte array where the read bytes will be stored.
     * @param off The start offset in the destination array at which data is written. Default is 0.
     * @param len The maximum number of bytes to read. Default is the size of the destination array.
     * @return The number of bytes read or -1 if the end of the stream is reached.
     * @throws IndexOutOfBoundsException If the specified offset and length exceed the bounds of the destination array.
     */
    fun read(
        dst: ByteArray,
        off: Int = 0,
        len: Int = dst.size,
    ): Int
}

internal class ByteChannelReadStream(
    private val channel: ReadableByteChannel,
    private val buffer: ByteBuffer,
) : ByteReadStream {
    override fun next(): Byte {
        if (!buffer.hasRemaining()) {
            refill()
        }

        return buffer.get()
    }

    override fun peak(): Int {
        if (!buffer.hasRemaining()) {
            try {
                refill()
            } catch (_: EOFException) {
                return -1
            }
        }

        return buffer.get(buffer.position()).toInt()
    }

    override fun read(
        dst: ByteArray,
        off: Int,
        len: Int,
    ): Int {
        if (off < 0 || len < 0 || off + len > dst.size) {
            throw IndexOutOfBoundsException("offset: $off, length: $len, array size: ${dst.size}")
        }

        if (len == 0) return 0

        if (!buffer.hasRemaining()) {
            try {
                refill()
            } catch (_: EOFException) {
                return -1
            }
        }

        val canRead = minOf(buffer.remaining(), len)
        buffer.get(dst, off, canRead)
        return canRead
    }

    private fun refill() {
        buffer.compact()
        val n = channel.read(buffer)
        buffer.flip()

        if (n == -1) {
            throw EOFException("Unexpected end of stream")
        }
    }
}
