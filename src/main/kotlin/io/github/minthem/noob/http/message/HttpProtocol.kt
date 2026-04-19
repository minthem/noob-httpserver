package io.github.minthem.noob.http.message

enum class HttpProtocol(private val version: String) {
    HTTP_1_0("HTTP/1.0"),
    HTTP_1_1("HTTP/1.1");

    fun version() = version

    companion object {
        fun fromString(value: String) =
            entries.firstOrNull { it.version == value } ?: throw IllegalArgumentException("Invalid protocol: $value")
    }
}
