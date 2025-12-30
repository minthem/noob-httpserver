package io.github.minthem.noobhttpserver.http

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.fileSize

internal interface ResponseBody {

    fun writeTo(channel: WritableByteChannel)

    fun defaultContentType(): String?

    fun contentLength(): Long
}

internal class EmptyResponseBody : ResponseBody {
    override fun writeTo(channel: WritableByteChannel) {
        // no op
    }

    override fun defaultContentType(): String? = null
    override fun contentLength(): Long = 0
}

internal class TextResponseBody(
    private val content: String,
    private val charset: Charset = Charsets.UTF_8
) : ResponseBody {

    private val bytes by lazy { content.toByteArray(charset) }

    override fun writeTo(channel: WritableByteChannel) {
        val buffer = ByteBuffer.wrap(bytes).position(0).limit(bytes.size)
        while (buffer.hasRemaining()) {
            channel.write(buffer)
        }
    }

    override fun defaultContentType(): String = "text/plain; charset=${charset.name()}"
    override fun contentLength(): Long = bytes.size.toLong()
}

internal class BinaryResponseBody(private val bytes: ByteArray) : ResponseBody {
    override fun writeTo(channel: WritableByteChannel) {
        val buffer = ByteBuffer.wrap(bytes).position(0).limit(bytes.size)
        while (buffer.hasRemaining()) {
            channel.write(buffer)
        }
    }

    override fun defaultContentType(): String = "application/octet-stream"

    override fun contentLength(): Long = bytes.size.toLong()

}

internal class FileResponseBody(
    private val path: Path,
    private val charset: Charset = Charsets.UTF_8
) : ResponseBody {

    override fun writeTo(channel: WritableByteChannel) {
        FileChannel.open(path, StandardOpenOption.READ).use { src ->
            var position = 0L
            val size = src.size()
            while (position < size) {
                val transferred = src.transferTo(position, size - position, channel)
                position += transferred
            }
        }
    }

    override fun defaultContentType(): String = "${Files.probeContentType(path)}; charset=${charset.name()}"
    override fun contentLength(): Long = path.fileSize()
}
