package io.github.minthem.noob.http.exception

import java.io.IOException

sealed class PayloadTooLargeException(
    message: String,
    val limitBytes: Long,
) : IOException(message)

class ChunkTooLargeException(
    val chunkSize: Long,
    maxChunkSizeBytes: Long,
) : PayloadTooLargeException(
        message = "Chunk size ($chunkSize bytes) exceeds limit of $maxChunkSizeBytes bytes",
        limitBytes = maxChunkSizeBytes,
    )

class ContentLengthTooLargeException(
    val contentLength: Long,
    maxContentLengthBytes: Long,
) : PayloadTooLargeException(
        message = "Content-Length ($contentLength bytes) exceeds limit of $maxContentLengthBytes bytes",
        limitBytes = maxContentLengthBytes,
    )

class BodySizeExceededException(
    val actualBytesRead: Long,
    maxBodySizeBytes: Long,
) : PayloadTooLargeException(
        message = "Stream body length exceeded limit of $maxBodySizeBytes bytes",
        limitBytes = maxBodySizeBytes,
    )
