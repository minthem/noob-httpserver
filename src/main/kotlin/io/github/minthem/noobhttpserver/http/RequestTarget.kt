package io.github.minthem.noobhttpserver.http

@ConsistentCopyVisibility
data class RequestTarget internal constructor(private val value: String) {

    init {
        require(OriginFormValidator.isValid(value)) { "Invalid request target: $value" }
    }

    private val pathQuery = value.split('?', limit = 2).let { it.first() to it.getOrNull(1) }

    val rawPath = pathQuery.first.ifEmpty { "/" }

    val rawQuery = pathQuery.second
}
