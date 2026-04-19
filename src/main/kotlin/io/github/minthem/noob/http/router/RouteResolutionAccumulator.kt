package io.github.minthem.noob.http.router

import io.github.minthem.noob.http.message.HttpMethod

internal class RouteResolutionAccumulator {

    private data class Candidate(
        val handler: Handler,
        val pathParams: Map<String, String>,
        val score: PathSpecificity
    )

    private var bestCandidate: Candidate? = null
    private val allowedMethods = linkedSetOf<HttpMethod>()

    fun considerMatch(handler: Handler, pathParams: Map<String, String>, score: PathSpecificity) {
        val candidate = Candidate(handler, pathParams, score)
        val currentBest = bestCandidate

        if (currentBest == null || candidate.score > currentBest.score) {
            bestCandidate = candidate
        }
    }

    fun considerMethodNotMatch(methods: Set<HttpMethod>) {
        allowedMethods.addAll(methods)
    }

    fun toRouteMatchResult(): RouteMatchResult {
        return when (val best = bestCandidate) {
            null -> {
                if (allowedMethods.isNotEmpty()) {
                    RouteMatchResult.MethodNotMatch(allowedMethods.toSet())
                } else {
                    RouteMatchResult.NotMatch
                }
            }

            else -> RouteMatchResult.Match(best.handler, best.pathParams, best.score)
        }
    }

    fun toRouterMatchResult(): RouterMatchResult {
        return when (val best = bestCandidate) {
            null -> {
                if (allowedMethods.isNotEmpty()) {
                    RouterMatchResult.MethodNotMatch(allowedMethods.toSet())
                } else {
                    RouterMatchResult.NotMatch
                }
            }

            else -> RouterMatchResult.Match(best.handler, best.pathParams)
        }
    }
}