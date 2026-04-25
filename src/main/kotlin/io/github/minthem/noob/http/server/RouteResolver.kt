package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.exception.MethodNotAllowException
import io.github.minthem.noob.http.exception.RouteNotFoundException
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.router.RouterMatchResult
import io.github.minthem.noob.http.router.RouterRegistry

internal class RouteResolver(
    private val registry: RouterRegistry,
) {
    fun resolve(request: HttpRequest): RouterMatchResult.Match {
        when (val result = registry.find(request)) {
            is RouterMatchResult.Match -> return result

            is RouterMatchResult.MethodNotMatch -> {
                throw MethodNotAllowException(request.method, result.allowedMethods)
            }

            is RouterMatchResult.NotMatch -> {
                throw RouteNotFoundException(request.method, request.path)
            }
        }
    }
}
