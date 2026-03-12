package io.github.minthem.noobhttpserver.http

import java.io.ByteArrayOutputStream

internal object UriDecoder {

    fun decodePath(path: String): String = decode(path, false)

    fun decodeQuery(query: String): String = decode(query, true)

    private fun decode(data: String, decodePlusAsSpace: Boolean = false): String {
        val result = StringBuilder(data.length)
        val bytes = ByteArrayOutputStream()
        var pos = 0

        fun flushBytes() {
            if (bytes.size() > 0) {
                result.append(bytes.toString(Charsets.UTF_8))
                bytes.reset()
            }
        }

        while (pos < data.length) {
            val c = data[pos++]

            if (c == '%') {
                if (data.length < pos + 2) {
                    throw IllegalArgumentException("Invalid percent-encoded string: $data")
                }

                try {
                    val hex = data.substring(pos, pos + 2).toInt(16)
                    bytes.write(hex)
                    pos += 2
                    continue
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Invalid percent-encoded string: $data", e)
                }
            }

            flushBytes()

            if (decodePlusAsSpace && c == '+') {
                result.append(' ')
            } else {
                result.append(c)
            }
        }

        flushBytes()
        return result.toString()
    }
}
