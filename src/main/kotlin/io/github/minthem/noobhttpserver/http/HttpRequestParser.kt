package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.exception.BadRequestException
import io.github.minthem.noobhttpserver.io.BodySourceInputStream
import io.github.minthem.noobhttpserver.io.ByteReadStream
import java.io.EOFException
import java.io.InputStream

internal class HttpRequestParser {

    fun parse(stream: ByteReadStream): HttpRequest {
        try {
            val method = readMethod(stream)
            val requestTarget = readRequestTarget(stream)
            val protocol = readProtocol(stream)
            val headers = readHeaders(stream)
            val bodyStream = getInputStreamForRequestBody(stream, headers)
            return HttpRequest(method, requestTarget, protocol, headers, bodyStream)
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(e.message ?: "Invalid request", e)
        }
    }

    private fun readMethod(stream: ByteReadStream): HttpMethod {
        val sb = StringBuilder()
        while (true) {
            val c = nextOrBadRequest(stream, "Unexpected end of stream while reading method")
            if (c == ' '.code.toByte()) {
                break
            }
            sb.append(c.toInt().toChar())
            if (sb.length > 10) {
                throw IllegalArgumentException("Invalid method")
            }
        }
        return HttpMethod.fromString(sb.toString())
    }

    private fun readRequestTarget(stream: ByteReadStream): RequestTarget {
        return RequestTargetParser.parseFromStream(stream) // TODO specified length
    }

    private fun readProtocol(stream: ByteReadStream): HttpProtocol {
        val sb = StringBuilder()
        while (true) {
            val c = nextOrBadRequest(stream, "Unexpected end of stream while reading protocol")
            if (c == '\r'.code.toByte()) {
                val next = peekOrBadRequest(stream, "Unexpected end of stream after CR in protocol")
                if (next == '\n'.code) {
                    nextOrBadRequest(stream, "Unexpected end of stream after CR in protocol")
                    break
                }

                // CRはProtocolは無いはず
                throw IllegalArgumentException("Invalid protocol")
            }
            sb.append(c.toInt().toChar())
            if (sb.length > 8) {
                throw IllegalArgumentException("Invalid protocol")
            }
        }
        return HttpProtocol.fromString(sb.toString())
    }

    private fun readHeaders(stream: ByteReadStream): HttpHeaders {
        return try {
            HttpHeadersParser.parse(stream)
        } catch (_: EOFException) {
            throw IllegalArgumentException("Unexpected end of stream while reading headers")
        }
    }

    private fun getInputStreamForRequestBody(stream: ByteReadStream, headers: HttpHeaders): InputStream {
        val contentLength = headers.contentLength
        val isChunked = headers["Transfer-Encoding"]?.equals("chunked", ignoreCase = true) ?: false


        if (contentLength != null && isChunked) {
            throw IllegalArgumentException("Content-Length and Transfer-Encoding headers are mutually exclusive")
        }

        val source = when {
            isChunked -> ChunkedBodySource(stream)
            else -> FixedLengthBodySource(stream, contentLength ?: 0)
        }

        return BodySourceInputStream(source)
    }

    private fun nextOrBadRequest(stream: ByteReadStream, message: String): Byte {
        return try {
            stream.next()
        } catch (_: EOFException) {
            throw IllegalArgumentException(message)
        }
    }

    private fun peekOrBadRequest(stream: ByteReadStream, message: String): Int {
        return try {
            stream.peak()
        } catch (_: EOFException) {
            throw IllegalArgumentException(message)
        }
    }
}
