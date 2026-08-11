package io.github.minthem.noob.http.message

import java.nio.channels.WritableByteChannel

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
