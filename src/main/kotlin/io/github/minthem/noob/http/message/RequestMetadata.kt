package io.github.minthem.noob.http.message

internal interface RequestMetadata {
    val protocol: HttpProtocol
    val headers: HttpHeaders
}

internal class FallbackRequestMetadata(
    override val protocol: HttpProtocol = HttpProtocol.HTTP_1_1,
    override val headers: HttpHeaders = HttpHeaders.EMPTY,
) : RequestMetadata
