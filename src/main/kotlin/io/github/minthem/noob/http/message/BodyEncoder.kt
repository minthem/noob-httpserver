package io.github.minthem.noob.http.message

import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel
import java.util.zip.GZIPOutputStream

internal interface BodyEncoder {
    fun encodeTo(
        destination: WritableByteChannel,
        body: BodyProducer,
    )

    val preservesContentLength: Boolean
    val contentEncoding: BodyEncoding?
}

internal class DefaultBodyEncoder : BodyEncoder {
    override fun encodeTo(
        destination: WritableByteChannel,
        body: BodyProducer,
    ) {
        body.writeTo(destination)
    }

    override val preservesContentLength: Boolean = true
    override val contentEncoding: BodyEncoding? = null
}

internal class GzipBodyEncoder : BodyEncoder {
    override fun encodeTo(
        destination: WritableByteChannel,
        body: BodyProducer,
    ) {
        val destStream = Channels.newOutputStream(destination)
        GZIPOutputStream(destStream).use {
            val gzipChannel = Channels.newChannel(it)
            body.writeTo(gzipChannel)
        }
    }

    override val preservesContentLength: Boolean = false
    override val contentEncoding: BodyEncoding = BodyEncoding.GZIP
}

internal object BodyEncoderFactory {
    fun create(encoding: BodyEncoding): BodyEncoder =
        when (encoding.type) {
            "gzip" -> GzipBodyEncoder()
            else -> DefaultBodyEncoder()
        }
}
