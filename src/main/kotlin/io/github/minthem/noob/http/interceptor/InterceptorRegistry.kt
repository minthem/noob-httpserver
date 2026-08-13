package io.github.minthem.noob.http.interceptor

import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.router.Context
import io.github.minthem.noob.http.router.Handler

/**
 * Holds an immutable snapshot of the interceptors used by a server.
 *
 * The interceptor order is preserved. The snapshot is created when this
 * registry is constructed and is shared by all request chains.
 */
internal class InterceptorRegistry(
    interceptors: List<Interceptor> = emptyList(),
) {
    private val interceptors = interceptors.toList()

    fun interceptHandler(
        context: Context,
        handler: Handler,
    ): HttpResponse {
        val initialChain = Chain(context, interceptors, 0, handler)
        return initialChain.proceed()
    }
}
