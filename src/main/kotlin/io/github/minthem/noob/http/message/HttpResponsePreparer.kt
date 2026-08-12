package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.codec.CodecRegistry
import io.github.minthem.noob.http.codec.NativeCodec
import io.github.minthem.noob.http.codec.StreamEncoder
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal class HttpResponsePreparer(
    clock: Clock = Clock.systemUTC(),
    private val codecRegistry: CodecRegistry = CodecRegistry(),
    decorators: List<HeaderDecorator> = listOf(),
) {
    private val decorators =
        listOf(
            DateHeaderDecorator(clock),
            ContentTypeHeaderDecorator(),
        ) + decorators

    fun prepare(
        request: RequestMetadata,
        response: HttpResponse,
    ): PreparedHttpResponse {
        val mutHeaders = response.headers.toMutable()
        val body = response.body

        val encoder = selectEncoding(request)
        val bodyModified = encoder !is NativeCodec
        if (bodyModified) {
            mutHeaders.add("content-encoding", encoder.id)
        }

        decorators.forEach { it.decorate(mutHeaders, request, response) }
        val useFixedLength = !bodyModified && body.contentLength != null

        val bodyWriter =
            if (useFixedLength) {
                mutHeaders.contentLength = body.contentLength
                mutHeaders["transfer-encoding"] = null
                FixedBodyWriter(body, encoder)
            } else {
                mutHeaders["transfer-encoding"] = "chunked"
                mutHeaders.contentLength = null
                ChunkedBodyWriter(body, encoder)
            }

        return PreparedHttpResponse(
            protocol = request.protocol,
            status = response.status,
            headers = mutHeaders.toImmutable(),
            bodyWriter = bodyWriter,
        )
    }

    private fun selectEncoding(request: RequestMetadata): StreamEncoder {
        val encoder =
            request.headers.acceptEncoding
                ?.filter { (it.quality ?: 1.0) > 0.0 }
                ?.sortedByDescending { (it.quality ?: 1.0) }
                ?.firstNotNullOfOrNull { codecRegistry.getEncoder(it.type) }
                ?: checkNotNull(codecRegistry.getEncoder(BodyEncoding.IDENTITY.type))

        return encoder
    }
}

internal class PreparedHttpResponse internal constructor(
    val protocol: HttpProtocol,
    val status: HttpStatus,
    val headers: HttpHeaders,
    val bodyWriter: BodyWriter,
)

internal interface HeaderDecorator {
    fun decorate(
        headers: MutableHttpHeaders,
        request: RequestMetadata,
        response: HttpResponse,
    )
}

internal class DateHeaderDecorator(
    private val clock: Clock,
) : HeaderDecorator {
    override fun decorate(
        headers: MutableHttpHeaders,
        request: RequestMetadata,
        response: HttpResponse,
    ) {
        val dt = clock.instant().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.RFC_1123_DATE_TIME)
        headers["date"] = dt
    }
}

internal class ContentTypeHeaderDecorator : HeaderDecorator {
    override fun decorate(
        headers: MutableHttpHeaders,
        request: RequestMetadata,
        response: HttpResponse,
    ) {
        if ("content-type" in headers) {
            return
        }
        headers.contentType = response.body.defaultContentType
    }
}
