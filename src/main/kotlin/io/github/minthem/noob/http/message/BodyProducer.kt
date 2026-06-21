package io.github.minthem.noob.http.message

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.fileSize

internal object BodyProducerFactory {
    fun create(spec: BodySpec): BodyProducer =
        when (spec) {
            is BodySpec.Empty -> EmptyBodyProducer
            is BodySpec.Text -> TextBodyProducer(spec)
            is BodySpec.Binary -> BinaryBodyProducer(spec)
            is BodySpec.File -> FileBodyProducer.create(spec)
            is BodySpec.Streaming -> StreamingBodyProducer(spec)
        }
}

internal interface BodyProducer {
    fun writeTo(destination: WritableByteChannel)

    val contentLength: Long?

    val defaultContentType: MediaType?
}

internal class FileBodyProducer private constructor(
    private val spec: BodySpec.File,
) : BodyProducer {
    override fun writeTo(destination: WritableByteChannel) {
        FileChannel.open(spec.path, StandardOpenOption.READ).use { source ->
            val size = source.size()
            var transferred = 0L
            while (transferred < size) {
                transferred += source.transferTo(transferred, size - transferred, destination)
            }
        }
    }

    override val contentLength: Long = spec.path.fileSize()

    override val defaultContentType: MediaType
        get() {
            val contentType = Files.probeContentType(spec.path) ?: "text/plain"
            return MediaType.parse(contentType).withCharset(spec.charset)
        }

    companion object {
        internal fun create(spec: BodySpec.File): BodyProducer {
            if (!Files.exists(spec.path)) {
                throw NoSuchFileException(file = spec.path.toFile(), reason = "File not found: ${spec.path}")
            }

            if (!Files.isReadable(spec.path)) {
                throw AccessDeniedException(file = spec.path.toFile(), reason = "File is not readable: ${spec.path}")
            }

            return FileBodyProducer(spec)
        }
    }
}

internal class BinaryBodyProducer internal constructor(
    private val spec: BodySpec.Binary,
) : BodyProducer {
    override fun writeTo(destination: WritableByteChannel) {
        val buffer = ByteBuffer.wrap(spec.bytes)
        while (buffer.hasRemaining()) {
            destination.write(buffer)
        }
    }

    override val contentLength: Long = spec.bytes.size.toLong()

    override val defaultContentType: MediaType = MediaType.parse("application/octet-stream")
}

internal class TextBodyProducer internal constructor(
    private val spec: BodySpec.Text,
) : BodyProducer {
    private val bytes by lazy { spec.text.toByteArray(spec.charset) }

    override fun writeTo(destination: WritableByteChannel) {
        val buffer = ByteBuffer.wrap(bytes)
        while (buffer.hasRemaining()) {
            destination.write(buffer)
        }
    }

    override val contentLength: Long = bytes.size.toLong()

    override val defaultContentType: MediaType = MediaType.parse("text/plain").withCharset(spec.charset)
}

internal object EmptyBodyProducer : BodyProducer {
    override fun writeTo(destination: WritableByteChannel) {
        // Do nothing
    }

    override val contentLength: Long = 0L

    override val defaultContentType: MediaType? = null
}

internal class StreamingBodyProducer internal constructor(
    private val spec: BodySpec.Streaming,
) : BodyProducer {
    override fun writeTo(destination: WritableByteChannel) {
        spec.source.use { stream ->
            stream.forEach {
                val buffer = ByteBuffer.wrap(it)
                while (buffer.hasRemaining()) {
                    destination.write(buffer)
                }
            }
        }
    }

    override val contentLength: Long? = null

    override val defaultContentType: MediaType = MediaType.OCTET_STREAM
}
