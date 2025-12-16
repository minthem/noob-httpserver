package io.github.minthem.http.request

import java.io.BufferedInputStream
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists

interface RequestBody : Closeable {

    fun contentLength(): Long

    fun openStream(): InputStream

    fun writeTo(out: OutputStream): Long {
        return openStream().use { it.copyTo(out) }
    }
}

class EmptyRequestBody internal constructor() : RequestBody {
    override fun contentLength(): Long = 0

    override fun openStream(): InputStream = InputStream.nullInputStream()

    override fun close() {
        // Nothing to do
    }
}

class InMemoryRequestBody internal constructor(bytes: ByteArray) : RequestBody {
    private val bytes = bytes.copyOf()

    override fun contentLength(): Long = bytes.size.toLong()

    override fun openStream(): InputStream = bytes.inputStream()

    override fun close() {
    }
}

class FileBackedRequestBody internal constructor(
    private val path: Path,
    private val deleteOnClose: Boolean = true
) : RequestBody {

    override fun contentLength(): Long {
        return Files.size(path)
    }

    override fun openStream(): InputStream = BufferedInputStream(Files.newInputStream(path))

    override fun close() {
        if (deleteOnClose) {
            path.deleteIfExists()
        }
    }
}