package io.github.minthem.noobhttpserver.http

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

internal object HttpHeaderValidator {

    private val FIELD_NAME_REGEX = Regex("^[A-Za-z0-9!#$%&'*+-.^_`|~]+$")

    fun isValidFieldName(name: String): Boolean = FIELD_NAME_REGEX.matches(name)

    fun isValidHeaderValue(value: String): Boolean {
        if (value.isEmpty()) return true
        return value.all(::isValidHeaderValueChar) && isValidHeaderValueChar(value.last())
    }

    private fun isValidHeaderValueChar(c: Char): Boolean {
        return isWhitespaceOrHorizontalTab(c) || isFieldChar(c)
    }

    private fun isWhitespaceOrHorizontalTab(c: Char): Boolean {
        return c == ' ' || c == '\t'
    }

    private fun isFieldChar(c: Char): Boolean {
        return c.code in 0x21..0x7E || c.code in 0x80..0xFF
    }
}

internal class SocketReadBuffer(
    private val socket: ReadableByteChannel, private val buffer: ByteBuffer
) {

    /**
     * 読み込むまでブロックする
     */
    fun readLine(): String {
        while (true) {
            val line = tryReadLine() ?: continue
            return line
        }
    }

    /**
     * 読み込めなかったらnullを返す
     */
    fun tryReadLine(): String? {
        val line = getLine()
        if (line != null) return line

        buffer.compact()
        val readN = socket.read(buffer)
        buffer.flip()

        if (readN == -1) {
            throw IllegalStateException("Unexpected end of stream")
        }

        return getLine()
    }

    private fun getLine(): String? {
        val lineEnd = findLineEnd()
        if (lineEnd != -1) {
            val stringLen = lineEnd - buffer.position() + 1
            val lineBuffer = ByteArray(stringLen)
            buffer.get(lineBuffer)

            return lineBuffer.toString(Charsets.US_ASCII).removeSuffix("\r\n")
        }

        return null
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