package io.github.minthem.noob.http.addon

import io.github.minthem.noob.http.exception.BadRequestException
import io.github.minthem.noob.http.message.BodyEncoding
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GzipBodySupport : ServerAddon {
    override fun install(registrar: ServerAddon.Registrar) {
        registrar.registerBodyEncoding(
            encoding = BodyEncoding.GZIP,
            preservesContentLength = false,
            decoder = ::decode,
            encoder = { GZIPOutputStream(it) },
        )
    }

    private fun decode(source: InputStream): InputStream =
        try {
            InvalidGzipMappingInputStream(GZIPInputStream(source))
        } catch (e: IOException) {
            throw BadRequestException("Invalid gzip request body", e)
        }
}

private class InvalidGzipMappingInputStream(
    source: InputStream,
) : FilterInputStream(source) {
    override fun read(): Int = mapInvalidGzip { super.read() }

    override fun read(
        bytes: ByteArray,
        offset: Int,
        length: Int,
    ): Int = mapInvalidGzip { super.read(bytes, offset, length) }

    override fun skip(byteCount: Long): Long = mapInvalidGzip { super.skip(byteCount) }

    private fun <T> mapInvalidGzip(action: () -> T): T =
        try {
            action()
        } catch (e: IOException) {
            throw BadRequestException("Invalid gzip request body", e)
        }
}
