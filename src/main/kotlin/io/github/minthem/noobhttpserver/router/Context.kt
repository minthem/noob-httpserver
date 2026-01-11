package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpHeaders
import io.github.minthem.noobhttpserver.http.HttpRequest
import io.github.minthem.noobhttpserver.http.MediaType
import io.github.minthem.noobhttpserver.http.MultipartBody
import java.io.Closeable
import java.io.InputStream

class Context internal constructor(
    private val req: HttpRequest,
    val pathParams: Map<String, String>
) : Closeable {

    private var readStream: Boolean = false

    private val cleanupActions = mutableListOf<() -> Unit>()

    val path: String by lazy { req.path.decodedPath }
    val headers: HttpHeaders = req.headers.toImmutable()
    val queryParams: Map<String, List<String>> by lazy { req.path.decodedQuery }

    private val bodyStream: InputStream
        get() {
            if (readStream) throw IllegalStateException("Body stream has already been read")
            readStream = true
            return req.bodyStream
        }

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

    fun bodyAsBytes(): ByteArray = bodyStream.readBytes()

    fun bodyAsMultipart(): MultipartBody {
        if (!(headers.contentType?.isCompatibleWith(MediaType.MULTIPART_FORM_DATA) ?: true)) {
            throw IllegalStateException("Content-Type must be multipart/form-data")
        }

        val boundary = headers.contentType?.parameters?.get("boundary")
            ?: throw IllegalStateException("Missing boundary parameter")

        if (readStream) {
            throw IllegalStateException("Body stream has already been read for multipart parsing")
        }

        val mp = MultipartBody(bodyStream, boundary)
        defer { mp.close() }
        return mp
    }

    fun defer(action: () -> Unit) = cleanupActions.add(action)

    override fun close() {
        cleanupActions.forEach { action ->
            try {
                action.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
