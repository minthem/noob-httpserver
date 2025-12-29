package io.github.minthem.noobhttpserver.http

import java.nio.charset.Charset
import java.nio.file.Path

data class HttpResponse(
    val status: HttpStatus = HttpStatus.OK,
    val headers: HttpHeaders = MutableHttpHeaders(),
) {

    internal var body: ResponseBody = EmptyResponseBody()

    fun bodyFromText(text: String, charset: Charset = Charsets.UTF_8) = apply {
        body = TextResponseBody(text, charset)
    }

    fun bodyFromBytes(bytes: ByteArray) = apply {
        body = BinaryResponseBody(bytes)
    }

    fun bodyFromFile(path: Path) = apply {
        body = FileResponseBody(path)
    }

    companion object {
        fun ok(header: HttpHeaders = MutableHttpHeaders()) = HttpResponse(HttpStatus.OK, header)
    }
}
