package io.github.minthem.noobhttpserver.http

import java.io.InputStream

class MultipartBody internal constructor(
    stream: InputStream,
    boundary: String
) {

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
}