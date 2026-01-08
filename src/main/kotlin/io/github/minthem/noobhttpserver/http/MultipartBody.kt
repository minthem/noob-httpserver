package io.github.minthem.noobhttpserver.http

import java.io.Closeable
import java.io.InputStream
import java.nio.file.Files

class MultipartBody internal constructor(
    stream: InputStream,
    boundary: String
) : Closeable {

    private val parser = MultipartBodyParser(stream, boundary)
    private val readParts = LinkedHashMap<String, Multipart>()

    fun part(name: String): Multipart? {
        if (readParts.containsKey(name)) {
            return readParts[name]
        }

        while (true) {
            val part = parser.nextPart() ?: return null

            readParts[part.name] = part

            if (part.name == name) {
                return part
            }
        }
    }

    fun forEachPart(block: (Multipart) -> Unit) {
        readParts.values.forEach(block)

        while (true) {
            val part = parser.nextPart() ?: return
            readParts[part.name] = part
            block(part)
        }
    }

    override fun close() {
        readParts.values.forEach { multipart ->
            if (multipart is Multipart.FileUpload && multipart.file != null) {
                synchronized(multipart.file) {
                    multipart.file.let { Files.deleteIfExists(it) }
                }

            }
        }

        readParts.clear()
    }
}
