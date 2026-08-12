package io.github.minthem.noob.http.codec

import io.github.minthem.noob.http.exception.BadRequestException
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GzipCodec(
    private val bufferSize: Int = 1024,
) : StreamCodec {
    init {
        require(bufferSize > 0) { "Buffer size must be positive" }
    }

    override val id: String = "gzip"

    override fun encode(output: OutputStream): OutputStream = GZIPOutputStream(output, bufferSize)

    override fun decode(input: InputStream): InputStream =
        mapInvalidGzip {
            InvalidGzipMappingInputStream(GZIPInputStream(input, bufferSize))
        }
}

private class InvalidGzipMappingInputStream(
    input: InputStream,
) : FilterInputStream(input) {
    override fun read(): Int = mapInvalidGzip { super.read() }

    override fun read(
        bytes: ByteArray,
        offset: Int,
        length: Int,
    ): Int = mapInvalidGzip { super.read(bytes, offset, length) }

    override fun skip(byteCount: Long): Long = mapInvalidGzip { super.skip(byteCount) }
}

private fun <T> mapInvalidGzip(action: () -> T): T =
    try {
        action()
    } catch (e: IOException) {
        throw BadRequestException("Invalid gzip request body", e)
    }
