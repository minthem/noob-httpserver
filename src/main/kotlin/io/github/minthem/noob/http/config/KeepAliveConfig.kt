package io.github.minthem.noob.http.config

data class KeepAliveConfig(
    val enabled: Boolean = true,
    val idleTimeoutMillis: Long = 3_000,
    val maxRequests: Int = 100,
) {
    init {
        require(idleTimeoutMillis > 0) { "Idle timeout must be positive" }
        require(maxRequests > 0) { "Max requests must be positive" }
    }
}
