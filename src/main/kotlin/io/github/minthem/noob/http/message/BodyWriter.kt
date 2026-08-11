package io.github.minthem.noob.http.message

import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.WritableByteChannel

internal interface BodyWriter {
    fun write(destination: WritableByteChannel)
}

internal class FixedBodyWriter(
    private val producer: BodyProducer,
    private val encoder: BodyEncoder,
) : BodyWriter {
    init {
        require(encoder.preservesContentLength) {
            "Encoder must preserve content length for fixed-length bodies"
        }

        require(producer.contentLength != null) {
            "Producer must have content length for fixed-length bodies"
        }
    }

    override fun write(destination: WritableByteChannel) {
        encoder.encodeTo(destination, producer)
    }
}

internal class ChunkedBodyWriter(
    private val producer: BodyProducer,
    private val encoder: BodyEncoder,
) : BodyWriter {
    override fun write(destination: WritableByteChannel) {
        ChunkedWritableByteChannel(destination).use { chunkedChannel ->
            encoder.encodeTo(chunkedChannel, producer)
        }
    }
}

private class ChunkedWritableByteChannel(
    private val destination: WritableByteChannel,
) : WritableByteChannel {
    private var open = true
    private var finished = false

    override fun write(src: ByteBuffer): Int {
        if (!open) throw ClosedChannelException()

        val size = src.remaining().toLong()
        val hexSize = size.toString(16)
        writeRaw("${hexSize}\r\n".encodeToByteArray())

        var totalWritten = 0
        while (src.hasRemaining()) {
            val written = destination.write(src)
            totalWritten += written
        }

        writeRaw("\r\n".encodeToByteArray())

        return totalWritten
    }

    override fun isOpen(): Boolean = open

    override fun close() {
        if (!finished) {
            writeRaw("0\r\n\r\n".encodeToByteArray())
            finished = true
        }

        if (open) {
            open = false
        }
    }

    private fun writeRaw(bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes)
        while (buffer.hasRemaining()) {
            destination.write(buffer)
        }
    }
}
