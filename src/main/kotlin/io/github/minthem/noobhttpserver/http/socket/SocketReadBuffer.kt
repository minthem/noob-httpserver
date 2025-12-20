package io.github.minthem.noobhttpserver.http.socket

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

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

    fun readBytes(dst: WritableByteChannel, length: Long) {
        var totalReadBytes = 0L

        while (totalReadBytes < length) {
            if (!buffer.hasRemaining()) {
                buffer.compact()
                val readN = socket.read(buffer)
                if (readN == -1) {
                    throw IllegalStateException("Unexpected end of stream")
                }
                buffer.flip()
            }

            val remainingToRead = length - totalReadBytes
            val oldLimit = buffer.limit()
            
            // バッファにある量と残りの必要量のうち、小さい方をリミットに設定
            val canRead = buffer.remaining().toLong().coerceAtMost(remainingToRead).toInt()
            
            buffer.limit(buffer.position() + canRead)
            val written = dst.write(buffer)
            buffer.limit(oldLimit)

            totalReadBytes += written
        }
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