package io.github.minthem.noobhttpserver.http

import java.nio.charset.Charset
import java.nio.file.Path

class HttpResponse private constructor(
    val status: HttpStatus,
    val headers: HttpHeaders,
    internal val body: BodyWriteExecutor
) {

    class Builder internal constructor() {
        var status: HttpStatus = HttpStatus.OK
        private var headers = MutableHttpHeaders()
        private var body: BodySpec = BodySpec.Empty

        fun header(key: String, value: String) = apply { headers.add(key, value) }
        fun header(vararg pairs: Pair<String, String>) = apply { headers.add(*pairs) }
        fun header(other: HttpHeaders) = apply {
            other.forEach { key, value -> headers.addAll(key, value) }
        }

        fun body(text: String, charset: Charset = Charsets.UTF_8) = apply { body = BodySpec.Text(text, charset) }
        fun body(bytes: ByteArray) = apply { body = BodySpec.Binary(bytes) }
        fun body(path: Path, charset: Charset = Charsets.UTF_8) = apply { body = BodySpec.File(path, charset) }
        fun body(source: CloseableSequence<ByteArray>) = apply { body = BodySpec.Chunked(source) }
        fun body(source: CloseableSequence<String>, charset: Charset = Charsets.UTF_8) = apply {
            body = BodySpec.Chunked(source.map { it.toByteArray(charset) }.asCloseable { source.close() })
        }

        fun build(): HttpResponse {
            val executor = BodyWriteExecutorFactory.create(body)

            if ("Content-Type" !in headers) {
                executor.defaultContentType()?.let {
                    headers.contentType = it
                }
            }

            if (body is BodySpec.Chunked) {
                headers["Transfer-Encoding"] = "chunked"
                if ("Content-Length" in headers) {
                    headers.remove("Content-Length")
                }
            } else {
                executor.contentLength()?.let { headers["Content-Length"] = it.toString() }
                if ("Transfer-Encoding" in headers) {
                    headers.remove("Transfer-Encoding")
                }
            }

            return HttpResponse(status, headers, executor)
        }
    }

    companion object {
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }
}
