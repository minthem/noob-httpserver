package io.github.minthem.noob.http.multipart

import io.github.minthem.noob.http.parser.HeaderValueParser
import io.github.minthem.noob.http.message.HeaderValue
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

@ConsistentCopyVisibility
data class ContentDisposition private constructor(
    val type: String,
    val parameters: Map<String, String> = emptyMap()
) {

    val name: String?
        get() = parameters["name"]

    val filename: String?
        get() = decodeExtendedFilename() ?: parameters["filename"]

    override fun toString(): String {
        return if (parameters.isEmpty()) {
            type
        } else {
            val params = parameters.entries.joinToString("; ") { "${it.key}=\"${it.value}\"" }
            "$type; $params"
        }
    }

    private fun decodeExtendedFilename(): String? {
        val filename = parameters["filename*"] ?: return null
        val parts = filename.split("'", limit = 3)
        if (parts.size != 3) return null

        val (charsetName, _, filenameRaw) = parts

        val charset = try {
            Charset.forName(charsetName)
        } catch (_: Exception) {
            return null
        }

        val bytes = ByteArrayOutputStream()
        var index = 0
        while (index < filenameRaw.length) {
            val c = filenameRaw[index]
            if (c == '%') {
                if (filenameRaw.length < index + 3) return null
                val hex = filenameRaw.substring(index + 1, index + 3)
                try {
                    val byteValue = hex.toInt(16)
                    bytes.write(byteValue)
                } catch (_: NumberFormatException) {
                    return null
                }
                index += 3
            } else {
                val isAttrChar =
                    c in '0'..'9' || c in 'A'..'Z' || c in 'a'..'z' || c in setOf(
                        '!',
                        '#',
                        '$',
                        '&',
                        '+',
                        '-',
                        '.',
                        '^',
                        '_',
                        '`',
                        '|',
                        '~'
                    )
                if (!isAttrChar) {
                    return null
                }
                bytes.write(c.code)
                index++
            }
        }
        return bytes.toByteArray().toString(charset)
    }

    companion object {
        fun parse(value: String): ContentDisposition {
            return fromHeaderValue(HeaderValueParser.parseSingle(value))
        }

        fun fromHeaderValue(headerValue: HeaderValue): ContentDisposition {
            val parameters = linkedMapOf<String, String>()
            headerValue.parameters.forEach { (name, value) ->
                value ?: return@forEach
                parameters[name.lowercase()] = value
            }

            return ContentDisposition(
                type = headerValue.value.lowercase(),
                parameters = parameters
            )
        }
    }
}
