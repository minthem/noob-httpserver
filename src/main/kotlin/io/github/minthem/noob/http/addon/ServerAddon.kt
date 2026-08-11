package io.github.minthem.noob.http.addon

import io.github.minthem.noob.http.message.BodyEncoding
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.router.Context
import java.io.InputStream
import java.io.OutputStream

/**
 * Adds optional behavior to a [io.github.minthem.noob.http.server.NoobHttpServer].
 * An add-on is installed once when the server is constructed and may be shared by concurrent requests.
 */
fun interface ServerAddon {
    fun install(registrar: Registrar)

    /** Registration API exposed to server add-ons. */
    interface Registrar {
        /** Registers request decoding and response encoding for one Content-Encoding. */
        fun registerBodyEncoding(
            encoding: BodyEncoding,
            preservesContentLength: Boolean,
            decoder: (InputStream) -> InputStream,
            encoder: (OutputStream) -> OutputStream,
        )

        /**
         * Wraps matched route handlers. Interceptors run in registration order and unwind in reverse order.
         * An interceptor may transform the response or return without invoking [next].
         */
        fun interceptRequests(interceptor: (Context, next: () -> HttpResponse) -> HttpResponse)
    }
}
