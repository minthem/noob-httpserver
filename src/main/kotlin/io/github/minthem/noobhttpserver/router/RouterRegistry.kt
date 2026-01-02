package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpRequest

internal class RouterRegistry {

    private val routers = mutableListOf<Router>()

    fun register(router: Router) {
        routers.add(router)
    }

    fun find(request: HttpRequest): RouteMatchResult {
        for (router in routers) {
            return when (val matchResult = router.findRoute(request)) {
                is RouteMatchResult.Match -> matchResult
                is RouteMatchResult.MethodNotAllowed -> matchResult
                is RouteMatchResult.NotFound -> continue
            }
        }

        return RouteMatchResult.NotFound
    }
}
