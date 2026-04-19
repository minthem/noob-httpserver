package io.github.minthem.noob.http.config

data class MultipartConfig(
    val memoryThresholdBytes: Int = 1 * 1024 * 1024
) {
    init {
        require(memoryThresholdBytes > 0) { "Memory threshold must be positive" }
    }
}