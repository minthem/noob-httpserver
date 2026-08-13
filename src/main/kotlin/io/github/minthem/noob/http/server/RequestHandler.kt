package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.exception.HttpResponseException
import io.github.minthem.noob.http.interceptor.InterceptorRegistry
import io.github.minthem.noob.http.io.ByteReadStream
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.parser.HttpRequestParser
import io.github.minthem.noob.http.router.Context
import org.slf4j.LoggerFactory

internal class RequestHandler(
    private val parser: HttpRequestParser,
    private val routeResolver: RouteResolver,
    private val interceptorRegistry: InterceptorRegistry,
) {
    /**
     * Processes an incoming HTTP request from the provided byte stream and generates a response.
     *
     * @param stream The input byte stream containing the raw HTTP request data.
     * @return A result object encapsulating the processed HTTP request and the corresponding response.
     *         If an error occurs during processing, a response generated from the exception is included.
     * @throws HttpResponseException If parsing fails before, a request object can be created.
     */
    fun process(stream: ByteReadStream): RequestHandlingResult {
        val request = parser.parse(stream)

        val response =
            try {
                val route = routeResolver.resolve(request)
                val response =
                    Context(request, route.pathParams).use {
                        interceptorRegistry.interceptHandler(
                            context = it,
                            handler = route.handler,
                        )
                    }

                response
            } catch (e: HttpResponseException) {
                logger.error("Error processing request: {}", e.message, e)
                e.httpResponse
            }

        return RequestHandlingResult(request, response)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RequestHandler::class.java)
    }
}

internal class RequestHandlingResult(
    val request: HttpRequest,
    val response: HttpResponse,
)
