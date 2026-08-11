package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.message.PreparedHttpResponse
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset

internal class HttpResponseWriter(
    private val responseHeaderBufferSize: Int,
) {
    fun write(
        writeChannel: WritableByteChannel,
        preparedHttpResponse: PreparedHttpResponse,
    ) {
        val buffer = ByteBuffer.allocate(responseHeaderBufferSize)

        ByteWriter(writeChannel, buffer).use {
            it.write(preparedHttpResponse.protocol.version())
            it.write(SPACE)
            it.write(preparedHttpResponse.status.code.toString())
            it.write(SPACE)
            it.write(preparedHttpResponse.status.reasonPhrase)
            it.write(CRLF)

            val headers = preparedHttpResponse.headers
            headers.forEach { key, values ->
                values.forEach { value ->
                    it.write(key)
                    it.write(FIELD_SEPARATOR)
                    it.write(value)
                    it.write(CRLF)
                }
            }
            it.write(CRLF)
        }

        preparedHttpResponse.bodyWriter.write(writeChannel)
    }

    companion object {
        private val CRLF = "\r\n".toByteArray()
        private val SPACE = " ".toByteArray()
        private val FIELD_SEPARATOR = ": ".toByteArray()
    }
}

private class ByteWriter(
    private val writeChannel: WritableByteChannel,
    private val buffer: ByteBuffer,
) : AutoCloseable {
    init {
        buffer.clear()
    }

    fun write(
        string: String,
        charset: Charset = Charsets.US_ASCII,
    ) {
        write(string.toByteArray(charset))
    }

    fun write(byteArray: ByteArray) {
        var written = 0
        while (written < byteArray.size) {
            if (!buffer.hasRemaining()) {
                flush()
            }

            val toWrite = minOf(buffer.remaining(), byteArray.size - written)
            buffer.put(byteArray, written, toWrite)
            written += toWrite
        }
    }

    private fun flush() {
        buffer.flip()
        while (buffer.hasRemaining()) {
            val written = writeChannel.write(buffer)
            if (written < 0) throw IllegalStateException("Unexpected end of stream")
        }
        buffer.clear()
    }

    override fun close() {
        if (buffer.position() > 0) {
            flush()
        }
    }
}
