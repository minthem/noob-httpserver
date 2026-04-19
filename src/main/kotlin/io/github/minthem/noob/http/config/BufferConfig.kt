package io.github.minthem.noob.http.config

data class BufferConfig(
    val requestBytes: Int = 8 * 1024,
    val responseHeaderBytes: Int = 2 * 1024
) {
    init {
        require(requestBytes > 0) { "Request buffer size must be positive" }
        require(responseHeaderBytes > 0) { "Response header buffer size must be positive" }
    }
}