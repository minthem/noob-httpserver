package io.github.minthem.noobhttpserver.http

@ConsistentCopyVisibility
data class RequestTarget internal constructor(private val value: String) {

    init {
        require(OriginFormValidator.isValid(value)) { "Invalid request target: $value" }
    }

    private val pathQuery = value.split('?', limit = 2).let { it.first() to it.getOrNull(1) }

    val rawPath = pathQuery.first.ifEmpty { "/" }

    val rawQuery = pathQuery.second

    val decodedPath: String by lazy { UriDecoder.decodePath(rawPath) }

    val decodedQuery: Map<String, List<String>> by lazy { decodedQuery() }

    private fun decodedQuery(): Map<String, List<String>> {
        val queries = rawQuery?.split('&') ?: emptyList()

        return queries.filter { it.isNotBlank() }.map {
            val pair = it.split('=', limit = 2)
            val decKey = UriDecoder.decodeQuery(pair[0])
            val decValue = pair.getOrNull(1)?.let { v -> UriDecoder.decodeQuery(v) } ?: ""
            decKey to decValue
        }.groupBy({ it.first }, { it.second })
    }
}
