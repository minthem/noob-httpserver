package io.github.minthem.noob.http.message

sealed class HttpHeaders {
    protected abstract val values: Map<String, List<String>>

    operator fun get(key: String): String? {
        val list = this.getAll(key)
        return list.firstOrNull()
    }

    fun getAll(key: String): List<String> = values[key.lowercase()] ?: emptyList()

    fun getJoined(key: String): String? = getAll(key).ifEmpty { null }?.joinToString(", ")

    operator fun contains(key: String): Boolean = values.containsKey(key.lowercase())

    fun forEach(action: (String, List<String>) -> Unit) {
        values.forEach { (key, values) -> action(key, values.toList()) }
    }

    abstract fun toImmutable(): ImmutableHttpHeaders

    abstract fun toMutable(): MutableHttpHeaders

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpHeaders) return false
        return values == other.values
    }

    override fun hashCode(): Int = values.hashCode()

    override fun toString(): String = values.toString()

    companion object {
        @JvmField
        val EMPTY: HttpHeaders = ImmutableHttpHeaders(emptyMap())

        fun of(initial: Map<String, List<String>>): HttpHeaders = ImmutableHttpHeaders(initial)

        fun of(vararg pairs: Pair<String, String>): HttpHeaders {
            val headers = MutableHttpHeaders()
            pairs.forEach { (key, value) ->
                headers.add(key, value)
            }
            return headers.toImmutable()
        }
    }
}

class ImmutableHttpHeaders(
    initial: Map<String, List<String>>,
) : HttpHeaders() {
    override val values: Map<String, List<String>> = initializeMap(initial)

    override fun toImmutable(): ImmutableHttpHeaders = this

    override fun toMutable(): MutableHttpHeaders = MutableHttpHeaders(values)
}

class MutableHttpHeaders(
    initial: Map<String, List<String>> = emptyMap(),
) : HttpHeaders() {
    private val mutableValues = initializeMap(initial)

    override val values: Map<String, List<String>>
        get() = mutableValues

    fun add(
        key: String,
        value: String,
    ) {
        val normalizedKey = key.lowercase()
        mutableValues.getOrPut(normalizedKey) { mutableListOf() }.add(value)
    }

    fun add(vararg pairs: Pair<String, String>) = pairs.forEach { (key, value) -> add(key, value) }

    fun addAll(
        key: String,
        values: List<String>,
    ) {
        val normalizedKey = key.lowercase()
        mutableValues.getOrPut(normalizedKey) { mutableListOf() }.addAll(values)
    }

    operator fun set(
        key: String,
        value: String?,
    ) {
        val normalizedKey = key.lowercase()
        if (value == null) {
            remove(normalizedKey)
        } else {
            mutableValues[normalizedKey] = mutableListOf(value)
        }
    }

    fun remove(key: String) {
        val normalizedKey = key.lowercase()
        mutableValues.remove(normalizedKey)
    }

    override fun toImmutable(): ImmutableHttpHeaders = ImmutableHttpHeaders(values)

    override fun toMutable(): MutableHttpHeaders = this
}

private fun initializeMap(initial: Map<String, List<String>>): MutableMap<String, MutableList<String>> {
    val map = HashMap<String, MutableList<String>>()
    initial.forEach { (key, values) ->
        val normalizedKey = key.lowercase()

        val existing = map[normalizedKey] ?: mutableListOf()
        existing.addAll(values)
        map[normalizedKey] = existing
    }
    return map
}
