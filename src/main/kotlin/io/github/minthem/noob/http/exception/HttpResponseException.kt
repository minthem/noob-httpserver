package io.github.minthem.noob.http.exception

import io.github.minthem.noob.http.message.HttpMethod
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.HttpStatus
import io.github.minthem.noob.http.message.RequestTarget

open class HttpResponseException(
    message: String? = null,
    cause: Throwable? = null,
    val httpResponse: HttpResponse,
) : RuntimeException(message, cause)

class BadRequestException(
    message: String,
    cause: Throwable? = null,
) : HttpResponseException(
        message = "Bad request: $message",
        cause = cause,
        httpResponse =
            HttpResponse.build {
                status = HttpStatus.BAD_REQUEST
                header("connection", "close")
            },
    )

internal class MethodNotAllowException(
    val requestMethod: HttpMethod,
    val allowedMethods: Set<HttpMethod>,
) : HttpResponseException(
        message = "Method $requestMethod is not allowed. Allowed methods: $allowedMethods",
        httpResponse =
            HttpResponse.build {
                status = HttpStatus.METHOD_NOT_ALLOWED
                header("connection", "close")
            },
    )

internal class RouteNotFoundException(
    val method: HttpMethod,
    val requestTarget: RequestTarget,
) : HttpResponseException(
        message = "No route found for $method $requestTarget",
        httpResponse =
            HttpResponse.build {
                status = HttpStatus.NOT_FOUND
                header("connection", "close")
            },
    )
