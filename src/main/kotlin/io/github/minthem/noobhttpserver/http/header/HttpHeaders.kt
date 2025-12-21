package io.github.minthem.noobhttpserver.http.header


sealed class HttpHeaders protected constructor(
    initial: Map<String, List<String>> = emptyMap()
) {

    protected open val values: Map<String, List<String>> = initializeMap(initial)

    fun getFirst(key: String): String? = this[key]?.firstOrNull()

    operator fun get(key: String): List<String>? = values[normalizeKey(key)]

    operator fun contains(key: String): Boolean = values.containsKey(normalizeKey(key))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpHeaders) return false
        return values == other.values
    }

    override fun hashCode(): Int {
        return values.hashCode()
    }

    override fun toString(): String = values.toString()
}

class ImmutableHttpHeaders(initial: Map<String, List<String>>) : HttpHeaders(initial)

class MutableHttpHeaders(initial: Map<String, List<String>> = emptyMap()) : HttpHeaders(initial) {

    private val mutableValues = initializeMap(initial)

    override val values: Map<String, List<String>>
        get() = mutableValues

    fun add(key: String, value: String) {
        isValidHeader(key, value)
        val normalizedKey = normalizeKey(key)
        mutableValues.getOrPut(normalizedKey) { mutableListOf() }.add(value)
    }

    fun set(key: String, value: String) {
        isValidHeader(key, value)
        val normalizedKey = normalizeKey(key)
        mutableValues[normalizedKey] = mutableListOf(value)
    }

    fun remove(key: String) {
        val normalizedKey = normalizeKey(key)
        mutableValues.remove(normalizedKey)
    }
}

private fun isValidHeader(name: String, value: String) {
    if (!HttpHeaderValidator.isValidFieldName(name)) {
        throw IllegalArgumentException("Invalid header name: $name")
    }
    if (!HttpHeaderValidator.isValidHeaderValue(value)) throw IllegalArgumentException(
        "Invalid header value: $value"
    )
}

private fun initializeMap(initial: Map<String, List<String>>): MutableMap<String, MutableList<String>> {
    val map = HashMap<String, MutableList<String>>()
    initial.forEach { (key, values) ->
        isValidHeader(key, values)
        val normalizedKey = normalizeKey(key)

        val existing = map[normalizedKey] ?: mutableListOf()
        existing.addAll(values)
        map[normalizedKey] = existing
    }
    return map
}

private fun isValidHeader(name: String, values: List<String>) {
    if (!HttpHeaderValidator.isValidFieldName(name)) {
        throw IllegalArgumentException("Invalid header name: $name")
    }

    values.forEach { value ->
        if (!HttpHeaderValidator.isValidHeaderValue(value)) throw IllegalArgumentException(
            "Invalid header value: $value"
        )
    }
}

private fun normalizeKey(key: String) = key.lowercase()
