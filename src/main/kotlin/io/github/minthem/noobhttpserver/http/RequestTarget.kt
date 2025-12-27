package io.github.minthem.noobhttpserver.http

data class RequestTarget(private val value: String) {

    init {
        require(OriginFormValidator.isValid(value)) { "Invalid request target: $value" }
    }
}
