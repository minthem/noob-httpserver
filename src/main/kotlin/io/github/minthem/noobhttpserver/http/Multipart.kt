package io.github.minthem.noobhttpserver.http

import java.io.InputStream
import java.nio.file.Path

// TODO ファイルかフィールドかチェックできると良さそう
sealed class Multipart(
    val name: String,
    val headers: HttpHeaders,
) {
    class FormField(
        name: String,
        headers: HttpHeaders,
        val value: String
    ) : Multipart(name, headers)

    class FileUpload(
        name: String,
        headers: HttpHeaders,
        val filename: String,
        val file: Path?,
        private val provider: () -> InputStream,
    ) : Multipart(name, headers) {

        fun asStream(): InputStream = provider()
    }
}
