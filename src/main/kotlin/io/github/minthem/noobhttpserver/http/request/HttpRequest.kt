package io.github.minthem.noobhttpserver.http.request

import io.github.minthem.noobhttpserver.http.header.HttpHeaders

data class HttpRequest(
    val method: String,
    val path: String,
    val protocol: String,
    val headers: HttpHeaders,
    val body: RequestBody,
)
