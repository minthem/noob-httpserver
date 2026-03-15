package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.exception.HttpResponseException
import io.github.minthem.noobhttpserver.io.ByteReadStream
import io.github.minthem.noobhttpserver.router.Context

internal class RequestHandler(
    private val parser: HttpRequestParser,
    private val routeResolver: RouteResolver
) {

    /**
     * Processes an incoming HTTP request from the provided byte stream and generates a response.
     *
     * @param stream The input byte stream containing the raw HTTP request data.
     * @return A result object encapsulating the processed HTTP request and the corresponding response.
     *         If an error occurs during processing, a response generated from the exception is included.
     * @throws HttpResponseException If parsing fails before a request object can be created.
     */
    fun process(stream: ByteReadStream): RequestHandlingResult {
        val request = parser.parse(stream)

        val response = try {
            val route = routeResolver.resolve(request)
            val response = Context(request, route.pathParams).use {
                route.handler(it)
            }

            response
        } catch (e: HttpResponseException) {
            e.printStackTrace() // TODO use logger library
            e.httpResponse
        }

        return RequestHandlingResult(request, response)
    }
}

internal class RequestHandlingResult(val request: HttpRequest, val response: HttpResponse)