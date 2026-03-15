package io.github.minthem.noobhttpserver.http

import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal object HttpResponseWriter {

    private val CRLF = "\r\n".toByteArray()
    private val SPACE = " ".toByteArray()
    private val FIELD_SEPARATOR = ": ".toByteArray()

    private val FIELD_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME

    fun write(
        writeChannel: WritableByteChannel,
        protocol: HttpProtocol,
        httpResponse: HttpResponse,
        now: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))
    ) {
        ByteWriter(writeChannel).use {
            it.write(protocol.version())
            it.write(SPACE)
            it.write(httpResponse.status.code.toString())
            it.write(SPACE)
            it.write(httpResponse.status.reasonPhrase)
            it.write(CRLF)

            val headers = httpResponse.headers.toMutable()
            if ("date" !in headers) {
                val utc = now.withZoneSameInstant(ZoneId.of("UTC"))
                headers["date"] = utc.format(FIELD_DATE_FORMATTER)
            }

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

        httpResponse.body.writeTo(writeChannel)
    }

}

private class ByteWriter(
    private val writeChannel: WritableByteChannel,
    private val buffer: ByteBuffer = ByteBuffer.allocate(2048) // TODO Parameterize
) : AutoCloseable {

    init {
        buffer.clear()
    }

    fun write(string: String, charset: Charset = Charsets.US_ASCII) {
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