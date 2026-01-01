package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpHeaders
import io.github.minthem.noobhttpserver.http.HttpRequest
import java.io.InputStream

class Context internal constructor(
    private val req: HttpRequest,
    val pathParams: Map<String, String>
) {

    val path: String by lazy { req.path.decodedPath }
    val headers: HttpHeaders = req.headers.toImmutable()
    val queryParams: Map<String, List<String>> by lazy { req.path.decodedQuery }
    val bodyStream: InputStream = req.bodyStream

    fun queryParam(key: String): String? = queryParams[key]?.firstOrNull()

    inline fun <reified T> queryParamAs(key: String): T? {
        val value = queryParam(key) ?: return null
        return when (T::class) {
            String::class -> value as T
            Int::class -> value.toInt() as T
            Long::class -> value.toLong() as T
            Double::class -> value.toDouble() as T
            Boolean::class -> value.toBoolean() as T
            else -> throw IllegalArgumentException("Unsupported type: ${T::class.simpleName}")
        }
    }

    inline fun <reified T> queryParamAs(key: String, default: T): T {
        return queryParamAs<T>(key) ?: default
    }

    fun bodyAsText(): String {
        val charset = headers.contentType?.charset ?: Charsets.UTF_8
        return String(
            bodyStream.readBytes(),
            charset
        )
    }

    fun bodyAsBytes(): ByteArray = req.bodyStream.readBytes()
}
