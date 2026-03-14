package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.exception.MethodNotAllowException
import io.github.minthem.noobhttpserver.exception.RouteNotFoundException
import io.github.minthem.noobhttpserver.router.RouterMatchResult
import io.github.minthem.noobhttpserver.router.RouterRegistry

internal class RouteResolver(
    private val registry: RouterRegistry
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