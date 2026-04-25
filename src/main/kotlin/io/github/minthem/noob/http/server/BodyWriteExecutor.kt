package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.message.BodySpec
import io.github.minthem.noob.http.message.MediaType
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.fileSize

internal object BodyWriteExecutorFactory {
    fun create(spec: BodySpec): BodyWriteExecutor =
        when (spec) {
            is BodySpec.Empty -> EmptyBodyExecutor
            is BodySpec.Text -> TextBodyExecutor(spec)
            is BodySpec.Binary -> BinaryBodyExecutor(spec)
            is BodySpec.File -> {
                if (!Files.exists(spec.path)) {
                    throw FileNotFoundException("File not found: ${spec.path}")
                }

                if (!Files.isReadable(spec.path)) {
                    throw IOException("File is not readable: ${spec.path}")
                }

                FileBodyExecutor(spec)
            }
            is BodySpec.Chunked -> ChunkedBodyExecutor(spec)
        }
}

internal interface BodyWriteExecutor {
    fun writeTo(destination: WritableByteChannel)

    fun contentLength(): Long?

    fun defaultContentType(): MediaType?
}

internal class FileBodyExecutor internal constructor(
    private val spec: BodySpec.File,
) : BodyWriteExecutor {
    override fun writeTo(destination: WritableByteChannel) {
        FileChannel.open(spec.path, StandardOpenOption.READ).use { src ->
            var position = 0L
            val size = src.size()
            while (position < size) {
                val transferred = src.transferTo(position, size - position, destination)
                position += transferred
            }
        }
    }

    override fun contentLength(): Long = spec.path.fileSize()

    override fun defaultContentType(): MediaType {
        val contentType = Files.probeContentType(spec.path) ?: "text/plain"
        return MediaType.parse("$contentType; charset=${spec.charset.name()}")
    }
}

internal class BinaryBodyExecutor internal constructor(
    private val spec: BodySpec.Binary,
) : BodyWriteExecutor {
    override fun writeTo(destination: WritableByteChannel) {
        val buffer = ByteBuffer.wrap(spec.bytes).position(0).limit(spec.bytes.size)
        while (buffer.hasRemaining()) {
            destination.write(buffer)
        }
    }

    override fun contentLength(): Long = spec.bytes.size.toLong()

    override fun defaultContentType(): MediaType = MediaType.parse("application/octet-stream")
}

internal class TextBodyExecutor internal constructor(
    private val spec: BodySpec.Text,
) : BodyWriteExecutor {
    private val bytes by lazy { spec.text.toByteArray(spec.charset) }

    override fun writeTo(destination: WritableByteChannel) {
        val binarySpec = BodySpec.Binary(bytes)
        BinaryBodyExecutor(binarySpec).writeTo(destination)
    }

    override fun contentLength(): Long = bytes.size.toLong()

    override fun defaultContentType(): MediaType = MediaType.parse("text/plain; charset=${spec.charset.name()}")
}

internal object EmptyBodyExecutor : BodyWriteExecutor {
    override fun writeTo(destination: WritableByteChannel) {
        // no op
    }

    override fun contentLength(): Long = 0L

    override fun defaultContentType(): MediaType? = null
}

internal class ChunkedBodyExecutor internal constructor(
    private val spec: BodySpec.Chunked,
) : BodyWriteExecutor {
    override fun writeTo(destination: WritableByteChannel) {
        val endLine = "\r\n"
        spec.source.use { source ->
            source.forEach { chunk ->
                val chunkSizeLine = chunk.size.toLong().toString(16) + endLine
                writeBytes(destination, chunkSizeLine.toByteArray())
                writeBytes(destination, chunk)
                writeBytes(destination, endLine.toByteArray())
            }

            val lastChunkSizeLine = "0${endLine}$endLine"
            writeBytes(destination, lastChunkSizeLine.toByteArray())
        }
    }

    private fun writeBytes(
        destination: WritableByteChannel,
        bytes: ByteArray,
    ) {
        val buffer = ByteBuffer.wrap(bytes).position(0).limit(bytes.size)
        while (buffer.hasRemaining()) {
            destination.write(buffer)
        }
    }

    override fun contentLength(): Long? = null

    override fun defaultContentType(): MediaType = MediaType.OCTET_STREAM
}
