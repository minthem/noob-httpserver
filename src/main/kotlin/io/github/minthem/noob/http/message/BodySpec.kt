package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.util.CloseableSequence
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.fileSize

sealed interface BodySpec {
    object Empty : BodySpec {
        override val contentLength: Long? = null
        override val defaultContentType: MediaType? = null
    }

    class Text(
        val text: String,
        val charset: Charset = Charsets.UTF_8,
    ) : BodySpec {
        private val bytes by lazy { text.toByteArray(charset) }

        override val contentLength: Long = bytes.size.toLong()
        override val defaultContentType: MediaType = MediaType.TEXT_PLAIN.withCharset(charset)
    }

    class Binary(
        val bytes: ByteArray,
    ) : BodySpec {
        override val contentLength: Long = bytes.size.toLong()
        override val defaultContentType: MediaType = MediaType.OCTET_STREAM
    }

    class File(
        val path: Path,
        val charset: Charset = Charsets.UTF_8,
    ) : BodySpec {
        private val type by lazy {
            val t = Files.probeContentType(path)
            t?.let { MediaType.parse(it).withCharset(charset) }
                ?: MediaType.TEXT_PLAIN.withCharset(charset)
        }

        override val contentLength: Long = path.fileSize()
        override val defaultContentType: MediaType = type
    }

    class Streaming(
        val source: CloseableSequence<ByteArray>,
    ) : BodySpec {
        override val contentLength: Long? = null
        override val defaultContentType: MediaType = MediaType.OCTET_STREAM
    }

    val contentLength: Long?

    val defaultContentType: MediaType?
}
