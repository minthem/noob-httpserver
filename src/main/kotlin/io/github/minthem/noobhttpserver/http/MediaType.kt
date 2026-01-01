package io.github.minthem.noobhttpserver.http

import java.nio.charset.Charset

data class MediaType(
    val type: String,
    val subtype: String,
    val parameters: Map<String, String> = emptyMap()
) {

    val charset: Charset? by lazy {
        parameters["charset"]?.let { Charset.forName(it) }
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
            val type = fullType.getOrElse(0, { "*" })
            val subtype = fullType.getOrElse(1, { "*" })
            val parameters = parts.drop(1).associate {
                val pair = it.split("=")
                pair[0].lowercase() to pair.getOrElse(1, { "" }).trim('"')
            }

            return MediaType(type, subtype, parameters)
        }
    }
}