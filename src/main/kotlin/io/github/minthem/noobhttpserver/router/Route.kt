package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpMethod
import io.github.minthem.noobhttpserver.http.HttpRequest
import io.github.minthem.noobhttpserver.http.RequestTarget


internal interface RouteComponent {
    fun match(request: HttpRequest): RouteMatchResult
}

internal class RouteGroup(
    private val pathPattern: PathPattern,
    private val routes: List<RouteComponent>
) : RouteComponent {
    override fun match(request: HttpRequest): RouteMatchResult {
        val groupMatchResult = pathPattern.match(request.path)
        if (groupMatchResult !is PathPatternMatchResult.Match) return RouteMatchResult.NotMatch

        val remaining = groupMatchResult.remainingPath ?: ""
        val subReq = request.withPath(RequestTarget(remaining))

        var matchResult: RouteMatchResult? = null
        for (route in routes) {
            matchResult = route.match(subReq)
            if (matchResult is RouteMatchResult.Match) {
                val pathParams = groupMatchResult.pathParams + matchResult.pathParams
                return RouteMatchResult.Match(matchResult.handler, pathParams)
            }
        }
        return matchResult ?: RouteMatchResult.NotMatch
    }
}

internal class Route(
    private val method: HttpMethod,
    private val pathPattern: PathPattern,
    private val handler: Handler
) : RouteComponent {
    override fun match(request: HttpRequest): RouteMatchResult {
        return when (val pathMatchResult = pathPattern.match(request.path)) {
            is PathPatternMatchResult.Match -> {
                if (request.method != method) {
                    return RouteMatchResult.MethodNotMatch(setOf(method))
                }

                RouteMatchResult.Match(
                    handler = handler,
                    pathParams = pathMatchResult.pathParams
                )
            }

            PathPatternMatchResult.NoMatch -> RouteMatchResult.NotMatch
        }
    }
}

internal sealed interface RouteMatchResult {
    class Match(val handler: Handler, val pathParams: Map<String, String>) : RouteMatchResult
    class MethodNotMatch(val allowedMethods: Set<HttpMethod>) : RouteMatchResult
    object NotMatch : RouteMatchResult
}
