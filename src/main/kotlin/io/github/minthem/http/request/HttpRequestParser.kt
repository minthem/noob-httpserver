package io.github.minthem.http.request

import io.github.minthem.http.header.HttpHeaders
import io.github.minthem.http.header.MutableHttpHeaders
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

internal class HttpRequestParser {

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
            var startPos = 0
            while (true) {
                val lineEnd = findLineEnd(buffer, startPos, buffer.position())
                if (lineEnd != -1) {
                    val line = String(buffer.array(), 0, lineEnd - 1, Charsets.US_ASCII)
                    buffer.flip()
                    buffer.position(lineEnd + 1)
                    buffer.compact()

                    return line
                }

                startPos = (buffer.position() - 2).coerceAtLeast(0)
                val readN = socket.read(buffer)
                if (readN == -1) {
                    throw IllegalStateException("Unexpected end of stream")
                }
            }
        }

        fun readNBytes(n: Long): ByteArray {
            val array = ByteArray(n.toInt())

            while (buffer.position() < n) {
                val readN = socket.read(buffer)
                if (readN == -1) {
                    throw IllegalStateException("Unexpected end of stream")
                }
            }

            buffer.flip()
            buffer.get(array)
            buffer.compact()
            return array
        }

        private fun findLineEnd(array: ByteBuffer, begin: Int, end: Int): Int {
            val cr = '\r'.code.toByte()
            val lf = '\n'.code.toByte()
            for (i in begin until end - 1) {
                if (array.get(i) == cr && array.get(i + 1) == lf) {
                    return i + 1
                }
            }

            return -1
        }
    }
}