package io.github.minthem.noob.http.router

import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.MediaType
import io.github.minthem.noob.http.multipart.MultipartBody
import io.github.minthem.noob.http.message.contentType
import java.io.Closeable
import java.io.InputStream

class Context internal constructor(
    private val req: HttpRequest,
    val pathParams: Map<String, String>
) : Closeable {

    val path: String by lazy { req.path.decodedPath }
    val headers: HttpHeaders = req.headers.toImmutable()
    val queryParams: Map<String, List<String>> by lazy { req.path.decodedQuery }

    private var readStream: Boolean = false
    private var closed: Boolean = false
    private val cleanupActions = mutableListOf<() -> Unit>()
    private val bodyStream: InputStream
        get() {
            if (readStream) throw IllegalStateException("Body stream has already been read")
            readStream = true
            return req.bodyStream
        }

    init {
        defer {
            req.drainBody()
        }
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
        val contentType = headers.contentType
            ?: throw IllegalStateException("Content-Type must be multipart/form-data")

        if (!contentType.isCompatibleWith(MediaType.MULTIPART_FORM_DATA)) {
            throw IllegalStateException("Content-Type must be multipart/form-data")
        }

        val boundary = contentType.parameters["boundary"]
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
        if (closed) return
        closed = true

        cleanupActions.asReversed().forEach { action ->
            try {
                action.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
