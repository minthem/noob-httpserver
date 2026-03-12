package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpRequest

internal class RouterRegistry {

    private val routers = mutableListOf<Router>()

    fun register(router: Router) {
        routers.add(router)
    }

    fun find(request: HttpRequest): RouterMatchResult {
        val allowedMethods = linkedSetOf<io.github.minthem.noobhttpserver.http.HttpMethod>()

        for (router in routers) {
            when (val matchResult = router.findRoute(request)) {
                is RouterMatchResult.Match -> return matchResult
                is RouterMatchResult.MethodNotMatch -> allowedMethods.addAll(matchResult.allowedMethods)
                is RouterMatchResult.NotMatch -> {}
            }
        }

        return if (allowedMethods.isNotEmpty()) {
            RouterMatchResult.MethodNotMatch(allowedMethods.toSet())
        } else {
            RouterMatchResult.NotMatch
        }
    }
}
