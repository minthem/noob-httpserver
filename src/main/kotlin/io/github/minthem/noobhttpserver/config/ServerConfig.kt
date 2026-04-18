package io.github.minthem.noobhttpserver.config

data class ServerConfig(
    val port: UShort = 8080u,
    val timeouts: TimeoutConfig = TimeoutConfig(),
    val buffers: BufferConfig = BufferConfig(),
    val httpLimits: HttpLimitsConfig = HttpLimitsConfig(),
    val multipart: MultipartConfig = MultipartConfig(),
    val keepAlive: KeepAliveConfig = KeepAliveConfig(),
) {
    init {
        require(port in 1u..65535u) { "Port must be positive and within valid range" }
    }
}