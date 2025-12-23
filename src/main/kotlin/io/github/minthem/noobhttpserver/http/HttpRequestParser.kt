package io.github.minthem.noobhttpserver.http

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

internal class HttpRequestParser {

    /**
     * バッファはreader modeで渡すこと
     */
    fun parse(channel: ReadableByteChannel, buffer: ByteBuffer): HttpRequest {
        val socketBuffer = SocketReadBuffer(channel, buffer)
        val (method, path, protocol) = parseRequestLine(socketBuffer)
        val headers = parseHeaders(socketBuffer)
        val stream = getInputStreamForRequestBody(socketBuffer, headers)
        return HttpRequest(method, path, protocol, headers, stream)
    }

    private fun parseRequestLine(socketBuffer: SocketReadBuffer): Triple<HttpMethod, String, HttpProtocol> {
        val requestLine = socketBuffer.readLine()
        val parts = requestLine.split(" ")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid request line: $requestLine")
        }

        val method = HttpMethod.fromString(parts[0])
        val protocol = HttpProtocol.fromString(parts[2])

        return Triple(method, parts[1], protocol)
    }

    private fun parseHeaders(socketBuffer: SocketReadBuffer): HttpHeaders {
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

    private fun getInputStreamForRequestBody(socketBuffer: SocketReadBuffer, headers: HttpHeaders): InputStream {
        val contentLength = headers.getFirst("Content-Length")?.toLong()
        val isChunked = headers.getFirst("Transfer-Encoding")?.equals("chunked", ignoreCase = true) ?: false

        if (contentLength != null && isChunked) {
            throw IllegalStateException("Content-Length and Transfer-Encoding headers are mutually exclusive")
        }

        val source = when {
            isChunked -> ChunkedBodySource(socketBuffer)
            else -> FixedLengthBodySource(socketBuffer, contentLength ?: 0)
        }

        return BodySourceInputStream(source)
    }
}

private class BodySourceInputStream(private val source: BodySource) : InputStream() {
    override fun read(): Int {
        val b = ByteArray(1)
        val n = source.read(b, 0, 1)
        return if (n == -1) {
            -1
        } else {
            b[0].toInt() and 0xff
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return source.read(b, off, len)
    }
}