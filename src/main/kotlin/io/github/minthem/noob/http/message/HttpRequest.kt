package io.github.minthem.noob.http.message

import java.io.InputStream

internal class HttpRequest(
    internal val method: HttpMethod,
    internal val path: RequestTarget,
    override val protocol: HttpProtocol,
    override val headers: HttpHeaders,
    internal val bodyStream: InputStream,
) : RequestMetadata {
    fun withPath(newPath: RequestTarget): HttpRequest = HttpRequest(method, newPath, protocol, headers, bodyStream)
}
