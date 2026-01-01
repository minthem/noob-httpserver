package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.exception.HttpResponseException
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
            throw HttpResponseException(
                message = "Invalid request line: $requestLine",
                httpResponse = HttpResponse.build {
                    status = HttpStatus.BAD_REQUEST
                    header("connection", "close")
                }
            )
        }

        return try {
            val method = HttpMethod.fromString(parts[0])
            val requestTarget = RequestTarget(parts[1])
            val protocol = HttpProtocol.fromString(parts[2])
            Triple(method, requestTarget, protocol)
        } catch (e: IllegalArgumentException) {
            throw HttpResponseException(
                message = "Invalid request line: $requestLine",
                cause = e,
                httpResponse = HttpResponse.build {
                    status = HttpStatus.BAD_REQUEST
                    header("connection", "close")
                }
            )
        }
    }

    private fun parseHeaders(socketBuffer: ByteChannelReader): HttpHeaders {
        return try {
            val headers = MutableHttpHeaders()

            while (true) {
                val line = socketBuffer.readLine()
                if (line.isEmpty()) {
                    break
                }
                val (name, value) = line.split(":", limit = 2)
                headers.add(name, value.trimStart())
            }

            headers
        } catch (e: IllegalStateException) {
            throw HttpResponseException(
                message = "Invalid headers",
                cause = e,
                httpResponse = HttpResponse.build {
                    status = HttpStatus.BAD_REQUEST
                    header("connection", "close")
                }
            )
        }
    }

    private fun getInputStreamForRequestBody(socketBuffer: ByteChannelReader, headers: HttpHeaders): InputStream {
        val contentLength = headers.getFirst("Content-Length")?.toLong()
        val isChunked = headers.getFirst("Transfer-Encoding")?.equals("chunked", ignoreCase = true) ?: false

        if (contentLength != null && isChunked) {
            throw HttpResponseException(
                message = "Content-Length and Transfer-Encoding headers are mutually exclusive",
                httpResponse = HttpResponse.build {
                    status = HttpStatus.BAD_REQUEST
                    header("connection", "close")
                }
            )
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