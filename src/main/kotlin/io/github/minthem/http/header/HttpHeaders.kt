package io.github.minthem.http.header

import java.util.TreeMap


sealed class HttpHeaders protected constructor(
    initial: Map<String, List<String>> = emptyMap()
) {

    protected open val values: Map<String, List<String>> = run {
        val map = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)
        initial.forEach { (key, values) ->
            isValidHeader(key, values)

            val existing = map[key]
            if (existing != null) {
                map[key] = existing + values
            } else {
                map[key] = values.toList()
            }
        }
        map
    }

    fun getFirst(name: String): String? = values[name]?.firstOrNull()

    operator fun get(name: String): List<String>? = values[name]

    operator fun contains(name: String): Boolean = values.containsKey(name)

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

    private val mutableValues = run {
        val map = TreeMap<String, MutableList<String>>(String.CASE_INSENSITIVE_ORDER)
        initial.forEach { (key, values) ->
            isValidHeader(key, values)

            val existing = map[key] ?: mutableListOf()
            existing.addAll(values)
            map[key] = existing
        }
        map
    }

    override val values: Map<String, List<String>>
        get() = mutableValues

    fun add(name: String, value: String) {
        isValidHeader(name, value)

        mutableValues.getOrPut(name) { mutableListOf() }.add(value)
    }

    fun set(name: String, value: String) {
        isValidHeader(name, value)
        mutableValues[name] = mutableListOf(value)
    }

    fun remove(name: String) {
        mutableValues.remove(name)
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