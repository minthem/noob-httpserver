package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.exception.HttpResponseException
import io.github.minthem.noobhttpserver.io.BodySourceInputStream
import io.github.minthem.noobhttpserver.io.ByteChannelReader
import io.github.minthem.noobhttpserver.io.ByteReadStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

internal class HttpRequestParser {

    fun parse(stream: ByteReadStream): HttpRequest {
        val method = readMethod(stream)
        val requestTarget = readRequestTarget(stream)
        val protocol = readProtocol(stream)
        val headers = readHeaders(stream)
        val bodyStream = getInputStreamForRequestBody(stream, headers)
        return HttpRequest(method, requestTarget, protocol, headers, bodyStream)
    }

    private fun readMethod(stream: ByteReadStream): HttpMethod {
        val sb = StringBuilder()
        while (true) {
            val c = stream.next()
            if (c == ' '.code.toByte()) {
                break
            }
            sb.append(c.toInt().toChar())
            if (sb.length > 10) {
                throw IllegalStateException("Invalid method")
            }
        }
        return HttpMethod.fromString(sb.toString())
    }

    private fun readRequestTarget(stream: ByteReadStream): RequestTarget {
        return RequestTargetParser.parse(stream) // TODO specified length
    }

    private fun readProtocol(stream: ByteReadStream): HttpProtocol {
        val sb = StringBuilder()
        while (true) {
            val c = stream.next()
            if (c == '\r'.code.toByte()) {
                if (stream.peak() == '\n'.code) {
                    stream.next()
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
        return HttpHeadersParser.parse(stream)
    }

    private fun getInputStreamForRequestBody(stream: ByteReadStream, headers: HttpHeaders): InputStream {
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
            isChunked -> ChunkedBodySource(stream)
            else -> FixedLengthBodySource(stream, contentLength ?: 0)
        }

        return BodySourceInputStream(source)
    }
}
