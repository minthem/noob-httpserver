package io.github.minthem.noobhttpserver.http.request

import io.github.minthem.noobhttpserver.http.HttpHeaders
import java.io.InputStream

data class HttpRequest(
    val method: String,
    val path: String,
    val protocol: String,
    val headers: HttpHeaders,
    val bodyStream: InputStream,
)

