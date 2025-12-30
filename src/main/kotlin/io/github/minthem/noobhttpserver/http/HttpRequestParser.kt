package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.io.ByteChannelReader
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

internal class HttpRequestParser {

    /**
     * バッファはreader modeで渡すこと
     */
    fun parse(channel: ReadableByteChannel, buffer: ByteBuffer): HttpRequest {
        val socketBuffer = ByteChannelReader(channel, buffer)
        val (method, requestTarget, protocol) = parseRequestLine(socketBuffer)
        val headers = parseHeaders(socketBuffer)
        val stream = getInputStreamForRequestBody(socketBuffer, headers)
        return HttpRequest(method, requestTarget, protocol, headers, stream)
    }

    private fun parseRequestLine(socketBuffer: ByteChannelReader): Triple<HttpMethod, RequestTarget, HttpProtocol> {
        val requestLine = socketBuffer.readLine()
        val parts = requestLine.split(" ")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid request line: $requestLine")
        }

        val method = HttpMethod.fromString(parts[0])
        val requestTarget = RequestTarget(parts[1])
        val protocol = HttpProtocol.fromString(parts[2])

        return Triple(method, requestTarget, protocol)
    }

    private fun parseHeaders(socketBuffer: ByteChannelReader): HttpHeaders {
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

    private fun getInputStreamForRequestBody(socketBuffer: ByteChannelReader, headers: HttpHeaders): InputStream {
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