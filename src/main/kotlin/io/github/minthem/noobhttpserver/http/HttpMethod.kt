package io.github.minthem.noobhttpserver.http

enum class HttpMethod(private val value: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    HEAD("HEAD");

    companion object {
        fun fromString(value: String) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException("Invalid method: $value")
    }
}
