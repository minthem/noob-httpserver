package io.github.minthem.noob.http.exception

import java.io.IOException

open class MalformedBodyException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

class MalformedChunkException(
    message: String,
    cause: Throwable? = null,
) : MalformedBodyException(message, cause)

class MalformedMultipartException(
    message: String,
    cause: Throwable? = null,
) : MalformedBodyException(message, cause)
