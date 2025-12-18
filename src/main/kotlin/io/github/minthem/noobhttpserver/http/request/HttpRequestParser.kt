package io.github.minthem.noobhttpserver.http.request

import io.github.minthem.noobhttpserver.http.header.HttpHeaders
import io.github.minthem.noobhttpserver.http.header.MutableHttpHeaders
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

internal class HttpRequestParser {

    /**
     * バッファはreader modeで渡すこと
     */
    fun parse(channel: ReadableByteChannel, buffer: ByteBuffer): HttpRequest {
        val socketBuffer = SocketChannelBuffer(channel, buffer)
        val (method, path, protocol) = parseRequestLine(socketBuffer)
        val headers = parseHeaders(socketBuffer)
        val body = readBody(socketBuffer, headers)
        return HttpRequest(method, path, protocol, headers, body)
    }

    private fun parseRequestLine(socketBuffer: SocketChannelBuffer): Triple<String, String, String> {
        val requestLine = socketBuffer.readLine()
        val parts = requestLine.split(" ")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid request line: $requestLine")
        }

        return Triple(parts[0], parts[1], parts[2])
    }

    private fun parseHeaders(socketBuffer: SocketChannelBuffer): HttpHeaders {
        val headers = MutableHttpHeaders()
        while (true) {
            val line = socketBuffer.readLine()
            if (line.isEmpty()) {
                break
            }
            val (name, value) = line.split(":", limit = 2)
            headers.add(name, value.trimStart())
        }

        return headers
    }

    private fun readBody(socketBuffer: SocketChannelBuffer, headers: HttpHeaders): RequestBody {
        val contentLength = headers.getFirst("Content-Length")?.toLongOrNull() ?: 0
        if (contentLength == 0L) {
            return EmptyRequestBody()
        }

        return InMemoryRequestBody(socketBuffer.readNBytes(contentLength))
    }

    private class SocketChannelBuffer(
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

        fun readNBytes(n: Long): ByteArray {
            val array = ByteArray(n.toInt())
            var readBytes = buffer.remaining()
            buffer.get(array, 0, readBytes)

            while (readBytes < n) {
                buffer.compact()
                val readN = socket.read(buffer)
                if (readN == -1) {
                    throw IllegalStateException("Unexpected end of stream")
                }
                buffer.flip()
                buffer.get(array, readBytes, readN)
                readBytes += readN
            }

            return array
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
}