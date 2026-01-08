package io.github.minthem.noobhttpserver.http


sealed class HttpHeaders {

    protected abstract val values: Map<String, List<String>>

    open val contentType: MediaType?
        get() {
            val contentType = getFirst("Content-Type") ?: return null
            return MediaType.parse(contentType)
        }

    fun getFirst(key: String): String? = this[key]?.firstOrNull()

    operator fun get(key: String): List<String>? = values[normalizeKey(key)]

    operator fun contains(key: String): Boolean = values.containsKey(normalizeKey(key))

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

    override fun hashCode(): Int {
        return values.hashCode()
    }

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

class ImmutableHttpHeaders(initial: Map<String, List<String>>) : HttpHeaders() {

    override val values: Map<String, List<String>> = initializeMap(initial)

    override fun toImmutable(): ImmutableHttpHeaders = this

    override fun toMutable(): MutableHttpHeaders = MutableHttpHeaders(values)
}

class MutableHttpHeaders(initial: Map<String, List<String>> = emptyMap()) : HttpHeaders() {

    private val mutableValues = initializeMap(initial)

    override val values: Map<String, List<String>>
        get() = mutableValues

    override var contentType: MediaType?
        get() = super.contentType
        set(value) {
            if (value != null) {
                set("Content-Type", value.toString())
            } else {
                remove("Content-Type")
            }
        }

    fun add(key: String, value: String) {
        isValidHeader(key, value)
        val normalizedKey = normalizeKey(key)
        mutableValues.getOrPut(normalizedKey) { mutableListOf() }.add(value)
    }

    fun add(vararg pairs: Pair<String, String>) = pairs.forEach { (key, value) -> add(key, value) }

    fun addAll(key: String, values: List<String>) {
        isValidHeader(key, values)
        val normalizedKey = normalizeKey(key)
        mutableValues.getOrPut(normalizedKey) { mutableListOf() }.addAll(values)
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

    override fun toImmutable(): ImmutableHttpHeaders = ImmutableHttpHeaders(values)

    override fun toMutable(): MutableHttpHeaders = this
}

private fun isValidHeader(name: String, value: String) {
    if (!HttpHeaderValidator.isValidFieldName(name)) {
        throw IllegalArgumentException("Invalid header name: $name")
    }
    if (!HttpHeaderValidator.isValidHeaderValue(value)) {
        throw IllegalArgumentException("Invalid header value: $value")
    }
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

    if (values.isEmpty()) throw IllegalArgumentException("Header values cannot be empty")

    values.forEach { value ->
        if (!HttpHeaderValidator.isValidHeaderValue(value)) throw IllegalArgumentException(
            "Invalid header value: $value"
        )
    }
}

private fun normalizeKey(key: String) = key.lowercase()
