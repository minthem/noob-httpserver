package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpMethod


internal class Route(
    val method: HttpMethod,
    val pathPattern: PathPattern,
    val handler: Handler
) {
}

internal sealed interface RouteMatchResult {
    class Match(val handler: Handler, val pathParams: Map<String, String>) : RouteMatchResult
    class MethodNotAllowed(val allowedMethods: Set<HttpMethod>) : RouteMatchResult
    object NotFound : RouteMatchResult
}
