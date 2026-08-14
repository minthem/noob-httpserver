package io.github.minthem.noob.http.parser

import io.github.minthem.noob.http.codec.CodecRegistry
import io.github.minthem.noob.http.config.HttpLimitsConfig
import io.github.minthem.noob.http.exception.ContentLengthTooLargeException
import io.github.minthem.noob.http.exception.RequestParseException
import io.github.minthem.noob.http.exception.UnsupportedBodyEncodingException
import io.github.minthem.noob.http.io.BodySourceAdapterInputStream
import io.github.minthem.noob.http.io.ByteReadStream
import io.github.minthem.noob.http.message.ChunkedBodySource
import io.github.minthem.noob.http.message.FixedLengthBodySource
import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpMethod
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.RequestTarget
import io.github.minthem.noob.http.message.RequestTargetParser
import io.github.minthem.noob.http.message.contentEncoding
import io.github.minthem.noob.http.message.contentLength
import java.io.EOFException
import java.io.InputStream

internal class HttpRequestParser(
    private val headerParser: HttpHeadersParser,
    private val config: HttpLimitsConfig,
    private val codecRegistry: CodecRegistry = CodecRegistry(),
) {
    /**
     * Parses an HTTP request from the given byte stream.
     *
     * @param stream The input stream containing the raw HTTP request data to be parsed.
     * @return An instance of [HttpRequest] containing the parsed HTTP method, request target,
     * protocol, headers, and body stream.
     * @throws RequestParseException If the request data is invalid or malformed.
     * @throws ContentLengthTooLargeException If the content length exceeds the maximum allowed limit.
     */
    fun parse(stream: ByteReadStream): HttpRequest {
        val method = readMethod(stream)
        val requestTarget = readRequestTarget(stream)
        val protocol = readProtocol(stream)
        val headers = readHeaders(stream)
        val bodyStream = getInputStreamForRequestBody(stream, headers)
        val stream = wrapBodyDecoder(bodyStream, headers)

        return HttpRequest(method, requestTarget, protocol, headers, stream)
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
                throw RequestParseException("Invalid method")
            }
        }
        return try {
            HttpMethod.fromString(sb.toString())
        } catch (e: IllegalArgumentException) {
            throw RequestParseException("Invalid method: $sb", e)
        }
    }

    private fun readRequestTarget(stream: ByteReadStream): RequestTarget =
        RequestTargetParser.parseFromStream(stream, config.maxRequestTargetBytes)

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
                throw RequestParseException("Invalid protocol")
            }
            sb.append(c.toInt().toChar())
            if (sb.length > 8) {
                throw RequestParseException("Invalid protocol")
            }
        }
        return try {
            HttpProtocol.fromString(sb.toString())
        } catch (e: IllegalArgumentException) {
            throw RequestParseException("Invalid protocol: $sb", e)
        }
    }

    private fun readHeaders(stream: ByteReadStream): HttpHeaders =
        try {
            headerParser.parse(stream)
        } catch (_: EOFException) {
            throw RequestParseException("Unexpected end of stream while reading headers")
        }

    private fun getInputStreamForRequestBody(
        stream: ByteReadStream,
        headers: HttpHeaders,
    ): InputStream {
        val contentLength =
            try {
                headers.contentLength
            } catch (e: IllegalArgumentException) {
                throw RequestParseException("Invalid Content-Length header", e)
            }
        val isChunked = headers["Transfer-Encoding"]?.equals("chunked", ignoreCase = true) ?: false

        if (contentLength != null && isChunked) {
            throw RequestParseException("Content-Length and Transfer-Encoding headers are mutually exclusive")
        }

        if (contentLength != null && config.maxRequestBodyBytes < contentLength) {
            throw ContentLengthTooLargeException(
                contentLength = contentLength,
                maxContentLengthBytes = config.maxRequestBodyBytes,
            )
        }

        val source =
            when {
                isChunked -> ChunkedBodySource(stream, config)
                else -> FixedLengthBodySource(stream, contentLength ?: 0)
            }

        return BodySourceAdapterInputStream(source)
    }

    private fun nextOrBadRequest(
        stream: ByteReadStream,
        message: String,
    ): Byte =
        try {
            stream.next()
        } catch (_: EOFException) {
            throw RequestParseException(message)
        }

    private fun peekOrBadRequest(
        stream: ByteReadStream,
        message: String,
    ): Int =
        try {
            stream.peak()
        } catch (_: EOFException) {
            throw RequestParseException(message)
        }

    private fun wrapBodyDecoder(
        bodyStream: InputStream,
        headers: HttpHeaders,
    ): InputStream {
        val encoding = headers.contentEncoding ?: return bodyStream
        val decoders =
            encoding.asReversed().map {
                val decoder =
                    codecRegistry.getDecoder(it.type)
                        ?: throw UnsupportedBodyEncodingException(it)
                decoder
            }

        var stream = bodyStream
        for (decoder in decoders) {
            stream = decoder.decode(stream)
        }

        return stream
    }
}
