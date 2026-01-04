package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpMethod
import io.github.minthem.noobhttpserver.http.HttpRequest
import io.github.minthem.noobhttpserver.http.HttpResponse

typealias Handler = (Context) -> HttpResponse


class Router(init: Router.() -> Unit) {

    private val components = mutableListOf<RouteComponent>()

    init {
        init()
    }

    fun get(pattern: String, handler: Handler) = addRoute(HttpMethod.GET, pattern, handler)
    fun post(pattern: String, handler: Handler) = addRoute(HttpMethod.POST, pattern, handler)
    fun put(pattern: String, handler: Handler) = addRoute(HttpMethod.PUT, pattern, handler)
    fun delete(pattern: String, handler: Handler) = addRoute(HttpMethod.DELETE, pattern, handler)
    fun head(pattern: String, handler: Handler) = addRoute(HttpMethod.HEAD, pattern, handler)

    fun group(pattern: String, init: Router.() -> Unit) {
        val subRouter = Router(init)

        components.add(
            RouteGroup(PathPattern.parse(pattern.trimEnd('/'), isPrefix = true), subRouter.components)
        )
    }

    private fun addRoute(method: HttpMethod, pattern: String, handler: Handler) {
        components.add(Route(method, PathPattern.parse(pattern.ifBlank { "/" }), handler))
    }

    internal fun findRoute(request: HttpRequest): RouteMatchResult {
        var matchResult: RouteMatchResult? = null

        for (component in components) {
            when (val mr = component.match(request)) {
                is RouteMatchResult.Match -> return mr
                is RouteMatchResult.MethodNotMatch -> {
                    matchResult = mr
                }

                is RouteMatchResult.NotMatch -> continue
            }
        }

        return matchResult ?: RouteMatchResult.NotMatch
    }
}
