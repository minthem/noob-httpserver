package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpMethod
import io.github.minthem.noobhttpserver.http.HttpRequest
import io.github.minthem.noobhttpserver.http.HttpResponse

typealias Handler = (Context) -> HttpResponse

class Router(init: Router.() -> Unit) {

    private val routes = mutableListOf<Route>()

    init {
        init()
    }

    private data class Route(
        val method: HttpMethod,
        val pattern: PathPattern,
        val handler: Handler
    )

    fun get(pattern: String, handler: Handler) = addRoute(HttpMethod.GET, pattern, handler)
    fun post(pattern: String, handler: Handler) = addRoute(HttpMethod.POST, pattern, handler)
    fun put(pattern: String, handler: Handler) = addRoute(HttpMethod.PUT, pattern, handler)
    fun delete(pattern: String, handler: Handler) = addRoute(HttpMethod.DELETE, pattern, handler)
    fun head(pattern: String, handler: Handler) = addRoute(HttpMethod.HEAD, pattern, handler)

    private fun addRoute(method: HttpMethod, pattern: String, handler: Handler) {
        routes.add(Route(method, PathPattern(pattern), handler))
    }

    internal fun match(request: HttpRequest): Handler? {
        return routes.find {
            it.method == request.method && it.pattern.match(request.path) is PathPatternMatchResult.Match
        }?.handler
    }
}
