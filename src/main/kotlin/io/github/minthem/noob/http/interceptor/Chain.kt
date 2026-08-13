package io.github.minthem.noob.http.interceptor

import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.router.Context
import io.github.minthem.noob.http.router.Handler

/**
 * Represents the current position in an interceptor chain.
 *
 * The same [context] instance is passed to all remaining interceptors and
 * to the final handler for the current request.
 *
 * Calling [proceed] invokes the next interceptor, or the handler when all
 * interceptors have completed. If [proceed] is not called, the chain is
 * short-circuited and the handler is not invoked.
 */
class Chain(
    val context: Context,
    private val interceptors: List<Interceptor>,
    private val index: Int,
    private val handler: Handler,
) {
    /**
     * Executes the current interceptor in the chain or handles the request if there are no more interceptors.
     * Creates a new chain instance with the updated context for the next interceptor, if applicable.
     *
     * @return The resulting {@code HttpResponse} after processing through the interceptors or the final handler.
     */
    fun proceed(): HttpResponse {
        if (index >= interceptors.size) {
            return handler(context)
        }

        val interceptor = interceptors[index]
        val nextChain = Chain(context, interceptors, index + 1, handler)
        return interceptor.intercept(nextChain)
    }
}
