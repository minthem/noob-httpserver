package io.github.minthem.noobhttpserver.config

data class TimeoutConfig(
    val readMillis: Long = 30_000,
    val writeMillis: Long = 30_000,
    val sessionMillis: Long = 120_000
) {
    init {
        require(readMillis > 0) { "Read timeout must be positive" }
        require(writeMillis > 0) { "Write timeout must be positive" }
        require(sessionMillis > 0) { "Session timeout must be positive" }
    }
}
