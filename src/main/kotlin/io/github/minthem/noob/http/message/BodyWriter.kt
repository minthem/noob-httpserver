package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.codec.NativeCodec
import io.github.minthem.noob.http.codec.StreamEncoder
import java.io.FilterOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ClosedChannelException
import java.nio.channels.WritableByteChannel

internal interface BodyWriter {
    fun write(destination: WritableByteChannel)
}

internal class FixedBodyWriter(
    private val producer: BodyProducer,
    private val encoder: StreamEncoder,
) : BodyWriter {
    init {
        require(producer.contentLength != null) {
            "Producer must have content length for fixed-length bodies"
        }
    }

    override fun write(destination: WritableByteChannel) {
        writeChannel(destination, producer, encoder)
    }
}

internal class ChunkedBodyWriter(
    private val producer: BodyProducer,
    private val encoder: StreamEncoder,
) : BodyWriter {
    override fun write(destination: WritableByteChannel) {
        ChunkedWritableByteChannel(destination).use { chunkedChannel ->
            writeChannel(chunkedChannel, producer, encoder)
        }
    }
}

private fun writeChannel(
    destination: WritableByteChannel,
    producer: BodyProducer,
    encoder: StreamEncoder,
) {
    if (encoder is NativeCodec) {
        producer.writeTo(destination)
        return
    }

    val output = NonClosingOutputStream(Channels.newOutputStream(destination))
    encoder.encode(output).use { encoded ->
        producer.writeTo(Channels.newChannel(encoded))
    }
}

private class NonClosingOutputStream(
    output: OutputStream,
) : FilterOutputStream(output) {
    override fun close() = flush()
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
