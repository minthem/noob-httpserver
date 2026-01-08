package io.github.minthem.noobhttpserver.http

import java.io.Closeable
import java.io.InputStream
import java.nio.file.Files
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
        private val file: Path?,
        private val provider: () -> InputStream,
    ) : Multipart(name, headers), Closeable {

        fun asStream(): InputStream = provider()

        override fun close() {
            file?.let { Files.deleteIfExists(it) }
        }
    }
}
