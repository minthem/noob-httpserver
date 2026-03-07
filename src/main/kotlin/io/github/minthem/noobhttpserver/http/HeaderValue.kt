package io.github.minthem.noobhttpserver.http

data class HeaderValue(
    val value: String,
    val parameters: Map<String, String?> = emptyMap()
) {

}