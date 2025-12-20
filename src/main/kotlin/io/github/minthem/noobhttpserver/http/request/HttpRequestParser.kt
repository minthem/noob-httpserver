package io.github.minthem.noobhttpserver.http.request

import io.github.minthem.noobhttpserver.http.header.HttpHeaders
import io.github.minthem.noobhttpserver.http.header.MutableHttpHeaders
import io.github.minthem.noobhttpserver.http.socket.SocketReadBuffer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel

internal class HttpRequestParser {

    /**
     * バッファはreader modeで渡すこと
     */
    fun parse(channel: ReadableByteChannel, buffer: ByteBuffer): HttpRequest {
        val socketBuffer = SocketReadBuffer(channel, buffer)
        val (method, path, protocol) = parseRequestLine(socketBuffer)
        val headers = parseHeaders(socketBuffer)
        val body = readBody(socketBuffer, headers)
        return HttpRequest(method, path, protocol, headers, body)
    }

    private fun parseRequestLine(socketBuffer: SocketReadBuffer): Triple<String, String, String> {
        val requestLine = socketBuffer.readLine()
        val parts = requestLine.split(" ")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid request line: $requestLine")
        }

        return Triple(parts[0], parts[1], parts[2])
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

    private fun readBody(socketBuffer: SocketReadBuffer, headers: HttpHeaders): RequestBody {
        val contentLength = headers.getFirst("Content-Length")?.toLongOrNull() ?: 0
        if (contentLength == 0L) {
            return EmptyRequestBody()
        }

        val byteArrayStream = ByteArrayOutputStream(contentLength.toInt())
        val writer = Channels.newChannel(byteArrayStream)
        socketBuffer.readBytes(writer, contentLength)

        return InMemoryRequestBody(byteArrayStream.toByteArray())
    }
}