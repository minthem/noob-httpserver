package io.github.minthem.noob.http.message

enum class HttpMethod(private val value: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    HEAD("HEAD");

    fun value() = value

    companion object {
        fun fromString(value: String) =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException("Invalid method: $value")
    }
}
