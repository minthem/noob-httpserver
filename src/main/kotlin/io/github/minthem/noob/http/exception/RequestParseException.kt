package io.github.minthem.noob.http.exception

import java.io.IOException

open class RequestParseException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)
