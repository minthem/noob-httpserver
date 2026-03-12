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
        val accumulator = RouteResolutionAccumulator()

        for (route in routes) {
            when (val matchResult = route.match(subReq)) {
                is RouteMatchResult.Match -> {
                    val mergedPathParams = groupMatchResult.pathParams + matchResult.pathParams
                    val mergedScore = pathPattern.specificity.append(matchResult.specificity)
                    accumulator.considerMatch(
                        handler = matchResult.handler,
                        pathParams = mergedPathParams,
                        score = mergedScore
                    )
                }

                is RouteMatchResult.MethodNotMatch -> {
                    accumulator.considerMethodNotMatch(matchResult.allowedMethods)
                }

                is RouteMatchResult.NotMatch -> {}
            }
        }

        return accumulator.toRouteMatchResult()
    }
}

internal class Route(
    private val method: HttpMethod,
    internal val pathPattern: PathPattern,
    private val handler: Handler
) : RouteComponent {
    override fun match(request: HttpRequest): RouteMatchResult {
        return when (val pathMatchResult = pathPattern.match(request.path)) {
            is PathPatternMatchResult.Match -> {
                if (request.method != method) {
                    RouteMatchResult.MethodNotMatch(setOf(method))
                } else {
                    RouteMatchResult.Match(
                        handler = handler,
                        pathParams = pathMatchResult.pathParams,
                        specificity = pathPattern.specificity
                    )
                }
            }

            PathPatternMatchResult.NoMatch -> RouteMatchResult.NotMatch
        }
    }
}

internal sealed interface RouteMatchResult {
    class Match(
        val handler: Handler,
        val pathParams: Map<String, String>,
        val specificity: PathSpecificity
    ) : RouteMatchResult

    class MethodNotMatch(
        val allowedMethods: Set<HttpMethod>
    ) : RouteMatchResult

    object NotMatch : RouteMatchResult
}
