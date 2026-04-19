package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.util.CloseableSequence
import java.nio.charset.Charset
import java.nio.file.Path

sealed interface BodySpec {
    object Empty : BodySpec
    class Text(val text: String, val charset: Charset = Charsets.UTF_8) : BodySpec
    class Binary(val bytes: ByteArray) : BodySpec
    class File(val path: Path, val charset: Charset = Charsets.UTF_8) : BodySpec
    class Chunked(val source: CloseableSequence<ByteArray>) : BodySpec
}
