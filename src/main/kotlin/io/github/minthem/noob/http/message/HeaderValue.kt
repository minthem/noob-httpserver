package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.multipart.ContentDisposition

data class HeaderValue(
    val value: String,
    val parameters: Map<String, String?> = emptyMap(),
)

private fun parseContentType(headers: HttpHeaders): MediaType? = headers["Content-Type"]?.let { MediaType.parse(it) }

val HttpHeaders.contentType: MediaType?
    get() = parseContentType(this)

var MutableHttpHeaders.contentType: MediaType?
    get() = parseContentType(this)
    set(value) {
        this["Content-Type"] = value?.toString()
    }

private fun parseContentLength(headers: HttpHeaders): Long? {
    val values = headers.getAll("Content-Length")
    if (values.isEmpty()) return null
    require(values.size == 1) { "Multiple Content-Length headers are not allowed" }

    val value =
        values.single().toLongOrNull()
            ?: throw IllegalArgumentException("Invalid Content-Length")

    require(value >= 0) { "Content-Length must be greater than or equal to 0" }
    return value
}

val HttpHeaders.contentLength: Long?
    get() = parseContentLength(this)

var MutableHttpHeaders.contentLength: Long?
    get() = parseContentLength(this)
    set(value) {
        require(value == null || value >= 0) { "Content-Length must be greater than or equal to 0" }
        this["Content-Length"] = value?.toString()
    }

private fun parseContentDisposition(headers: HttpHeaders): ContentDisposition? =
    headers["Content-Disposition"]?.let {
        ContentDisposition.parse(it)
    }

val HttpHeaders.contentDisposition: ContentDisposition?
    get() = parseContentDisposition(this)

var MutableHttpHeaders.contentDisposition: ContentDisposition?
    get() = parseContentDisposition(this)
    set(value) {
        this["Content-Disposition"] = value?.toString()
    }

private fun parseAcceptEncoding(headers: HttpHeaders): List<BodyEncoding>? =
    headers.getJoined("Accept-Encoding")?.let {
        BodyEncoding.parseAll(it)
    }

val HttpHeaders.acceptEncoding: List<BodyEncoding>?
    get() = parseAcceptEncoding(this)

var MutableHttpHeaders.acceptEncoding: List<BodyEncoding>?
    get() = parseAcceptEncoding(this)
    set(value) {
        val joined = value?.joinToString(",") { it.toString() }
        this["Accept-Encoding"] = if (joined.isNullOrEmpty()) null else joined
    }

private fun parseContentEncoding(headers: HttpHeaders): List<BodyEncoding>? =
    headers.getJoined("Content-Encoding")?.let {
        BodyEncoding.parseAll(it)
    }

val HttpHeaders.contentEncoding: List<BodyEncoding>?
    get() = parseContentEncoding(this)

var MutableHttpHeaders.contentEncoding: List<BodyEncoding>?
    get() = parseContentEncoding(this)
    set(value) {
        val joined = value?.joinToString(",") { it.toString() }
        this["Content-Encoding"] = if (joined.isNullOrEmpty()) null else joined
    }
