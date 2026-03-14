package io.github.minthem.noobhttpserver.http

internal object KeepAliveStrategy {
    fun shouldKeepAlive(request: HttpRequest, response: HttpResponse): Boolean {
        val requestAllowKeepAlive = when (request.protocol) {
            HttpProtocol.HTTP_1_0 -> {
                request.headers["Connection"]?.contains("keep-alive", ignoreCase = true) ?: false
            }
            HttpProtocol.HTTP_1_1 -> {
                val isClose = request.headers["Connection"]?.contains("close", ignoreCase = true) ?: false
                !isClose
            }
        }

        val specificClose = response.headers["Connection"]?.contains("close", ignoreCase = true) ?: false

        return requestAllowKeepAlive && !specificClose
    }
}