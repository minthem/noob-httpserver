package io.github.minthem.noob.http.addon

import io.github.minthem.noob.http.exception.UnsupportedBodyEncodingException
import io.github.minthem.noob.http.message.BodyEncoder
import io.github.minthem.noob.http.message.BodyEncoding
import io.github.minthem.noob.http.message.BodyProducer
import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.contentEncoding
import io.github.minthem.noob.http.router.Context
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel

internal class AddonRegistry(
    addons: List<ServerAddon> = emptyList(),
) : ServerAddon.Registrar {
    private val bodyEncodings = linkedMapOf<String, RegisteredBodyEncoding>()
    private val requestInterceptors = mutableListOf<(Context, () -> HttpResponse) -> HttpResponse>()
    private var acceptingRegistrations = true

    init {
        try {
            addons.forEach { it.install(this) }
        } finally {
            acceptingRegistrations = false
        }
    }

    override fun registerBodyEncoding(
        encoding: BodyEncoding,
        preservesContentLength: Boolean,
        decoder: (InputStream) -> InputStream,
        encoder: (OutputStream) -> OutputStream,
    ) {
        check(acceptingRegistrations) { "Add-on registration is already complete" }
        val type = encoding.type
        require(type != "identity") { "The identity body codec is provided by the server" }
        require(type !in bodyEncodings) { "Body encoding is already registered: $type" }
        bodyEncodings[type] =
            RegisteredBodyEncoding(
                encoding = encoding,
                preservesContentLength = preservesContentLength,
                decoder = decoder,
                streamEncoder = encoder,
            )
    }

    override fun interceptRequests(interceptor: (Context, () -> HttpResponse) -> HttpResponse) {
        check(acceptingRegistrations) { "Add-on registration is already complete" }
        requestInterceptors.add(interceptor)
    }

    fun decodeRequestBody(
        headers: HttpHeaders,
        source: InputStream,
    ): InputStream =
        headers.contentEncoding.orEmpty().asReversed().fold(source) { decoded, encoding ->
            when (encoding.type) {
                "identity" -> decoded
                else -> bodyEncodings[encoding.type]?.decoder?.invoke(decoded) ?: throw UnsupportedBodyEncodingException(encoding)
            }
        }

    fun responseBodyEncoders(): Map<String, BodyEncoder> = bodyEncodings.mapValues { (_, encoding) -> encoding.encoder() }

    fun interceptRequest(
        context: Context,
        handler: () -> HttpResponse,
    ): HttpResponse {
        val chain =
            requestInterceptors.asReversed().fold(handler) { next, interceptor ->
                { interceptor(context, next) }
            }
        return chain()
    }
}

private class RegisteredBodyEncoding(
    val encoding: BodyEncoding,
    val preservesContentLength: Boolean,
    val decoder: (InputStream) -> InputStream,
    private val streamEncoder: (OutputStream) -> OutputStream,
) {
    fun encoder(): BodyEncoder =
        object : BodyEncoder {
            override fun encodeTo(
                destination: WritableByteChannel,
                body: BodyProducer,
            ) {
                streamEncoder(Channels.newOutputStream(destination)).use { encodedStream ->
                    body.writeTo(Channels.newChannel(encodedStream))
                }
            }

            override val preservesContentLength: Boolean = this@RegisteredBodyEncoding.preservesContentLength
            override val contentEncoding: BodyEncoding = encoding
        }
}
