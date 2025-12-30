package io.github.minthem.noobhttpserver.http

import java.io.InputStream

internal class HttpRequest(
    internal val method: HttpMethod,
    internal val path: RequestTarget,
    internal val protocol: HttpProtocol,
    internal val headers: HttpHeaders,
    internal val bodyStream: InputStream,
)
