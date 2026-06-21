package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.parser.HeaderValueParser
import io.github.minthem.noob.http.parser.HeaderValueSplitter

@ConsistentCopyVisibility
data class BodyEncoding private constructor(
    val type: String,
    val quality: Double? = null,
) : Comparable<BodyEncoding> {
    override fun toString(): String = type + (quality?.let { ";q=$it" } ?: "")

    override operator fun compareTo(other: BodyEncoding): Int = (quality ?: 1.0).compareTo(other.quality ?: 1.0)

    companion object {
        val IDENTITY: BodyEncoding = BodyEncoding("identity")
        val GZIP: BodyEncoding = BodyEncoding("gzip")

        private val TOKEN_REGEX = Regex("^[!#$%&'*+.^_`|~0-9a-zA-Z-]+$")

        fun parse(value: String): BodyEncoding = parse(HeaderValueParser.parseSingle(value))

        fun parseAll(value: String): List<BodyEncoding> =
            HeaderValueSplitter.split(value, ',').mapNotNull {
                try {
                    parse(it)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }

        private fun parse(value: HeaderValue): BodyEncoding {
            val type = value.value.lowercase()
            val quality = value.parameters["q"]

            if (type.isEmpty()) {
                throw IllegalArgumentException("Invalid body encoding (type is empty)")
            }

            if (type != "*" && !TOKEN_REGEX.matches(type)) {
                throw IllegalArgumentException("Invalid body encoding (type contains invalid characters: $type)")
            }

            val parsedQuality =
                try {
                    quality?.toDouble()?.also { q ->
                        if (q !in 0.0..1.0) {
                            throw IllegalArgumentException("Invalid body encoding quality value (must be between 0.0 and 1.0): $q")
                        }
                    }
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Invalid body encoding quality value: ${quality ?: "null"}", e)
                }

            return BodyEncoding(type, parsedQuality)
        }
    }
}
