package io.github.minthem.noob.http.message

import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal class HttpResponsePreparer(
    private val clock: Clock = Clock.systemUTC(),
    private val contentNegotiator: ContentNegotiator = ContentNegotiator(),
    private val decorators: List<HeaderDecorator> =
        listOf(
            DateHeaderDecorator(clock),
            ContentTypeHeaderDecorator(),
        ),
) {
    fun prepare(
        request: RequestMetadata,
        response: HttpResponse,
    ): PreparedHttpResponse {
        val mutHeaders = response.headers.toMutable()
        val body = response.body

        val encoder = contentNegotiator.selectEncoder(request)
        if (encoder.contentEncoding != null) {
            mutHeaders.add("content-encoding", encoder.contentEncoding.toString())
        }

        decorators.forEach { it.decorate(mutHeaders, request, response) }
        val useFixedLength = encoder.preservesContentLength && body.contentLength != null

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
}

internal class PreparedHttpResponse internal constructor(
    val protocol: HttpProtocol,
    val status: HttpStatus,
    val headers: HttpHeaders,
    val bodyWriter: BodyWriter,
)

internal class ContentNegotiator(
    encoders: Map<String, BodyEncoder> = emptyMap(),
) {
    private val encoders = mapOf("identity" to DefaultBodyEncoder()) + encoders

    fun selectEncoder(request: RequestMetadata): BodyEncoder {
        val selected =
            request.headers.acceptEncoding
                ?.asSequence()
                ?.filter { (it.quality ?: 1.0) > 0.0 }
                ?.sortedDescending()
                ?.firstNotNullOfOrNull { encoders[it.type] }

        return selected ?: DefaultBodyEncoder()
    }
}

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
