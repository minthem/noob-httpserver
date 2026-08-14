package io.github.minthem.noob.http.config

data class HttpLimitsConfig(
    val maxRequestTargetBytes: Int = 8 * 1024,
    val maxHeaderSectionBytes: Int = 16 * 1024,
    val maxHeaderNameBytes: Int = 256,
    val maxHeaderValueBytes: Int = 8 * 1024,
    val maxHeaderCount: Int = 100,
    val maxRequestBodyBytes: Long = 16 * 1024 * 1024,
    val maxChunkSizeBytes: Long = 8 * 1024 * 1024,
) {
    init {
        require(maxRequestTargetBytes > 0) { "Max request target bytes must be positive" }
        require(maxHeaderSectionBytes > 0) { "Max header section bytes must be positive" }
        require(maxHeaderNameBytes > 0) { "Max header name bytes must be positive" }
        require(maxHeaderValueBytes > 0) { "Max header value bytes must be positive" }
        require(maxHeaderCount > 0) { "Max header count must be positive" }
        require(maxRequestBodyBytes > 0) { "Max request body bytes must be positive" }
        require(maxChunkSizeBytes > 0) { "Max chunk size bytes must be positive" }
    }
}
