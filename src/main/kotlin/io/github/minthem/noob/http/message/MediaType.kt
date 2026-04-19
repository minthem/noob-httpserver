package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.parser.HeaderValueParser
import java.nio.charset.Charset

@ConsistentCopyVisibility
data class MediaType private constructor(
    val type: String,
    val subtype: String,
    val parameters: Map<String, String> = emptyMap()
) {

    val charset: Charset? by lazy {
        parameters["charset"]?.let { Charset.forName(it) }
    }

    fun isCompatibleWith(other: MediaType): Boolean {
        val typeMatch = type == "*" || other.type == "*" || type == other.type
        val subtypeMatch = subtype == "*" || other.subtype == "*" || subtype == other.subtype
        return typeMatch && subtypeMatch
    }

    override fun toString(): String {
        return if (parameters.isEmpty()) {
            "$type/$subtype"
        } else {
            val params = parameters.entries.joinToString("; ") { "${it.key}=\"${it.value}\"" }
            "$type/$subtype; $params"
        }
    }

    companion object {
        fun parse(value: String): MediaType {
            return fromHeaderValue(HeaderValueParser.parseSingle(value))
        }

        fun fromHeaderValue(headerValue: HeaderValue): MediaType {
            val fullType = headerValue.value.split("/", limit = 2)

            val type = fullType.getOrElse(0) { "*" }.lowercase()
            val subtype = fullType.getOrElse(1) { "*" }.lowercase()

            val parameters = headerValue.parameters.mapValues { (_, v) -> v ?: "" }

            return MediaType(type, subtype, parameters)
        }

        val MULTIPART_FORM_DATA = MediaType("multipart", "form-data")
        val OCTET_STREAM = MediaType("application", "octet-stream")
    }
}
