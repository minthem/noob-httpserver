package io.github.minthem.noobhttpserver.http

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
            val params = parameters.entries.joinToString("; ") { "${it.key}=${it.value}" }
            "$type/$subtype; $params"
        }
    }

    companion object {
        fun parse(contentType: String): MediaType {
            val parts = contentType.split(";").map { it.trim() }
            val fullType = parts[0].split("/")

            val type = fullType.getOrElse(0) { "*" }.lowercase()
            val subtype = fullType.getOrElse(1) { "*" }.lowercase()

            val parameters = parts.drop(1).associate {
                val pair = it.split("=")
                pair[0].lowercase() to pair.getOrElse(1) { "" }.trim('"')
            }

            return MediaType(type, subtype, parameters)
        }

        val MULTIPART_FORM_DATA = MediaType("multipart", "form-data")
    }
}