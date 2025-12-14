package io.github.minthem.http.header

import java.util.TreeMap


sealed class HttpHeaders protected constructor(
    initial: Map<String, List<String>> = emptyMap()
) {

    protected open val values: Map<String, List<String>> = run {
        val map = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)
        initial.forEach { (key, value) ->
            val existing = map[key]
            if (existing != null) {
                map[key] = existing + value
            } else {
                map[key] = value.toList()
            }
        }
        map
    }

    fun getFirst(name: String): String? = values[name]?.firstOrNull()

    operator fun get(name: String): List<String>? = values[name]

    operator fun contains(name: String): Boolean = values.containsKey(name)
}

class ImmutableHttpHeaders(initial: Map<String, List<String>>) : HttpHeaders(initial)

class MutableHttpHeaders(initial: Map<String, List<String>> = emptyMap()) : HttpHeaders(initial) {

    private val mutableValues = run {
        val map = TreeMap<String, MutableList<String>>(String.CASE_INSENSITIVE_ORDER)
        initial.forEach { (key, value) ->
            val existing = map[key] ?: mutableListOf()
            existing.addAll(value)
            map[key] = existing
        }
        map
    }

    override val values: Map<String, List<String>>
        get() = mutableValues

    fun add(name: String, value: String) {
        mutableValues.getOrPut(name) { mutableListOf() }.add(value)
    }

    fun set(name: String, value: String) {
        mutableValues[name] = mutableListOf(value)
    }

    fun remove(name: String) {
        mutableValues.remove(name)
    }
}