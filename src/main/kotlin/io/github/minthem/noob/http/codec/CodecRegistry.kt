package io.github.minthem.noob.http.codec

class CodecRegistry(
    codecs: List<StreamCodec> = emptyList(),
) {
    private val supportedCodecs: Map<String, StreamCodec> =
        buildMap {
            val nativeCodec = NativeCodec()
            put(nativeCodec.id, nativeCodec)

            codecs.forEach { codec ->
                require(TOKEN_REGEX.matches(codec.id)) { "Codec id must be a valid HTTP token: ${codec.id}" }
                require(codec.id == codec.id.lowercase()) { "Codec id must be lowercase: ${codec.id}" }
                require(codec.id !in this) { "Codec is already registered: ${codec.id}" }
                put(codec.id, codec)
            }
        }

    /**
     * Retrieves a `StreamEncoder` associated with the specified codec name.
     *
     * @param name The identifier for the desired codec.
     * @return The `StreamEncoder` for the given codec name or `null` if no matching codec is found.
     */
    fun getEncoder(name: String): StreamEncoder? = supportedCodecs[name]

    /**
     * Retrieves a `StreamDecoder` instance associated with the specified codec name.
     *
     * @param name The identifier for the desired codec.
     * @return The `StreamDecoder` for the given codec name or `null` if no matching codec is found.
     */
    fun getDecoder(name: String): StreamDecoder? = supportedCodecs[name]

    private companion object {
        val TOKEN_REGEX = Regex("^[!#$%&'*+.^_`|~0-9a-z-]+$")
    }
}
