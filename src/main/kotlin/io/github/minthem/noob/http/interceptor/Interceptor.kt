package io.github.minthem.noob.http.interceptor

import io.github.minthem.noob.http.message.HttpResponse

interface Interceptor {
    /**
     * Intercepts an HTTP request and response.
     *
     * The interceptor can inspect the request through [Chain.context], return a
     * response immediately, or call [Chain.proceed] to continue processing.
     *
     * Interceptors are executed in registration order. An interceptor instance
     * may be used concurrently for multiple requests, so request-specific state
     * should be kept in local variables rather than instance fields.
     *
     * [Chain.proceed] should normally be called at most once for each invocation
     * of this method.
     *
     * @param chain The {@code Chain} instance representing the current state of the interceptor chain.
     *              It provides access to the request's context, the remaining interceptors, and the handler.
     * @return The resulting {@code HttpResponse} after processing the request within this interceptor.
     */
    fun intercept(chain: Chain): HttpResponse
}
