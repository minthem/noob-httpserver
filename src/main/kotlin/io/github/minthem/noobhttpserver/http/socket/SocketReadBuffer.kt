package io.github.minthem.noobhttpserver.http.socket

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

internal class SocketReadBuffer(
    private val socket: ReadableByteChannel, private val buffer: ByteBuffer
) {
    fun readLine(): String {
        while (true) {
            val lineEnd = findLineEnd()
            if (lineEnd != -1) {
                val stringLen = lineEnd - buffer.position() + 1
                val lineBuffer = ByteArray(stringLen)
                buffer.get(lineBuffer)

                return lineBuffer.toString(Charsets.US_ASCII).removeSuffix("\r\n")
            }
            // ない場合は、未読み取り部分を前詰めしてソケットから取得する
            buffer.compact()
            val readN = socket.read(buffer)
            if (readN == -1) {
                throw IllegalStateException("Unexpected end of stream")
            }
            buffer.flip()
        }
    }

    fun read(arr: ByteArray, offset: Int = 0, length: Int = arr.size): Int {
        if (offset < 0 || length < 0 || offset + length > arr.size) {
            throw IndexOutOfBoundsException("offset: $offset, length: $length, array size: ${arr.size}")
        }
        if (length == 0) return 0

        // バッファが空ならソケットから補充する（少なくとも1回は試みる）
        if (!buffer.hasRemaining()) {
            buffer.compact()
            val readN = socket.read(buffer)
            if (readN == -1) return -1 // EOF
            buffer.flip()
        }

        // バッファにある分と要求された分のうち、小さい方を読み出す
        val canRead = minOf(buffer.remaining(), length)
        buffer.get(arr, offset, canRead)

        return canRead
    }

    private fun findLineEnd(): Int {
        val cr = '\r'.code.toByte()
        val lf = '\n'.code.toByte()
        for (i in buffer.position() until buffer.limit() - 1) {
            if (buffer.get(i) == cr && buffer.get(i + 1) == lf) {
                return i + 1
            }
        }

        return -1
    }
}