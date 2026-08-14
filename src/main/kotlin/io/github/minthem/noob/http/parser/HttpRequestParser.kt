package io.github.minthem.noob.http.parser

import io.github.minthem.noob.http.codec.CodecRegistry
import io.github.minthem.noob.http.config.HttpLimitsConfig
import io.github.minthem.noob.http.exception.BadRequestException
import io.github.minthem.noob.http.exception.ContentLengthTooLargeException
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
    fun parse(stream: ByteReadStream): HttpRequest {
        try {
            val method = readMethod(stream)
            val requestTarget = readRequestTarget(stream)
            val protocol = readProtocol(stream)
            val headers = readHeaders(stream)
            val bodyStream = getInputStreamForRequestBody(stream, headers)
            val stream = wrapBodyDecoder(bodyStream, headers)

            return HttpRequest(method, requestTarget, protocol, headers, stream)
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
                throw IllegalArgumentException("Invalid protocol")
            }
            sb.append(c.toInt().toChar())
            if (sb.length > 8) {
                throw IllegalArgumentException("Invalid protocol")
            }
        }
        return HttpProtocol.fromString(sb.toString())
    }

    private fun readHeaders(stream: ByteReadStream): HttpHeaders =
        try {
            headerParser.parse(stream)
        } catch (_: EOFException) {
            throw IllegalArgumentException("Unexpected end of stream while reading headers")
        }

    private fun getInputStreamForRequestBody(
        stream: ByteReadStream,
        headers: HttpHeaders,
    ): InputStream {
        val contentLength = headers.contentLength
        val isChunked = headers["Transfer-Encoding"]?.equals("chunked", ignoreCase = true) ?: false

        if (contentLength != null && isChunked) {
            throw IllegalArgumentException("Content-Length and Transfer-Encoding headers are mutually exclusive")
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
            throw IllegalArgumentException(message)
        }

    private fun peekOrBadRequest(
        stream: ByteReadStream,
        message: String,
    ): Int =
        try {
            stream.peak()
        } catch (_: EOFException) {
            throw IllegalArgumentException(message)
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
