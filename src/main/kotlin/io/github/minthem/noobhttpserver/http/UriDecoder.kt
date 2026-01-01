package io.github.minthem.noobhttpserver.http

import java.io.ByteArrayOutputStream

internal object UriDecoder {

    fun decodePath(path: String): String = decode(path, false)

    fun decodeQuery(query: String): String = decode(query, true)

    private fun decode(data: String, decodePlusAsSpace: Boolean = false): String {
        val stream = ByteArrayOutputStream()
        var pos = 0

        while (pos < data.length) {
            val c = data[pos++]
            if (c == '%') {
                if (data.length < pos + 2) {
                    throw IllegalArgumentException("Invalid percent-encoded string: $data")
                }

                try {
                    val hex = data.substring(pos, pos + 2).toInt(16)
                    stream.write(hex)
                    pos += 2
                    continue
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Invalid percent-encoded string: $data", e)
                }
            }

            stream.write(c.code)
        }

        return stream.toString(Charsets.UTF_8)
    }
}
