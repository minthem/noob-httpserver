package io.github.minthem.noobhttpserver.exception

import io.github.minthem.noobhttpserver.http.HttpResponse

class HttpResponseException(
    message: String? = null,
    cause: Throwable? = null,
    val httpResponse: HttpResponse
) : RuntimeException(message, cause)
