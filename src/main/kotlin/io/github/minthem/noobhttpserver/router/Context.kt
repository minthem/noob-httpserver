package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpRequest

class Context internal constructor(
    private val req: HttpRequest,
    val pathParams: Map<String, String>
) {

    val path: String by lazy { req.path.decodedPath }

    val queryParams: Map<String, List<String>> by lazy { req.path.decodedQuery }
}
