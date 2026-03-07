package io.github.minthem.noobhttpserver.http


private fun parseContentType(headers: HttpHeaders): MediaType? {
    return headers["Content-Type"]?.let { MediaType.parse(it) }
}

val HttpHeaders.contentType: MediaType?
    get() = parseContentType(this)

var MutableHttpHeaders.contentType: MediaType?
    get() = parseContentType(this)
    set(value) {
        this["Content-Type"] = value?.toString()
    }


private fun parseContentLength(headers: HttpHeaders): Long? {
    return headers["Content-Length"]?.toLong()
}

val HttpHeaders.contentLength: Long?
    get() = parseContentLength(this)

var MutableHttpHeaders.contentLength: Long?
    get() = parseContentLength(this)
    set(value) {
        this["Content-Length"] = value?.toString()
    }


private fun parseContentDisposition(headers: HttpHeaders): ContentDisposition? {
    return headers["Content-Disposition"]?.let { ContentDisposition.parse(it) }
}

val HttpHeaders.contentDisposition: ContentDisposition?
    get() = parseContentDisposition(this)

var MutableHttpHeaders.contentDisposition: ContentDisposition?
    get() = parseContentDisposition(this)
    set(value) {
        this["Content-Disposition"] = value?.toString()
    }