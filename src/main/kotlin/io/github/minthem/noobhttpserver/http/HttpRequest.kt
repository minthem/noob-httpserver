package io.github.minthem.noobhttpserver.http

import java.io.InputStream

data class HttpRequest(
    val method: HttpMethod,
    val path: String,
    val protocol: HttpProtocol,
    val headers: HttpHeaders,
    val bodyStream: InputStream,
)