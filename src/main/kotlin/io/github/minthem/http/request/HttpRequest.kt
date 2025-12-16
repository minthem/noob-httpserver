package io.github.minthem.http.request

import io.github.minthem.http.header.HttpHeaders

data class HttpRequest(
    val method: String,
    val path: String,
    val protocol: String,
    val headers: HttpHeaders,
    val body: RequestBody,
)
