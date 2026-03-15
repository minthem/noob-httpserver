package io.github.minthem.noobhttpserver.exception

import io.github.minthem.noobhttpserver.http.HttpMethod
import io.github.minthem.noobhttpserver.http.HttpResponse
import io.github.minthem.noobhttpserver.http.HttpStatus
import io.github.minthem.noobhttpserver.http.RequestTarget

open class HttpResponseException(
    message: String? = null,
    cause: Throwable? = null,
    val httpResponse: HttpResponse
) : RuntimeException(message, cause)


internal class MethodNotAllowException(
    val requestMethod: HttpMethod,
    val allowedMethods: Set<HttpMethod>
) : HttpResponseException(
    message = "Method $requestMethod is not allowed. Allowed methods: $allowedMethods",
    httpResponse = HttpResponse.build {
        status = HttpStatus.METHOD_NOT_ALLOWED
        header("connection", "close")
    }
)

internal class RouteNotFoundException(
    val method: HttpMethod,
    val requestTarget: RequestTarget
) : HttpResponseException(
    message = "No route found for $method $requestTarget",
    httpResponse = HttpResponse.build {
        status = HttpStatus.NOT_FOUND
        header("connection", "close")
    }
)
