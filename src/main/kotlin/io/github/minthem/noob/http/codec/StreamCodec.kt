package io.github.minthem.noob.http.codec

import java.io.InputStream
import java.io.OutputStream

interface StreamEncoder {
    val id: String

    fun encode(output: OutputStream): OutputStream
}

interface StreamDecoder {
    val id: String

    fun decode(input: InputStream): InputStream
}

interface StreamCodec :
    StreamEncoder,
    StreamDecoder
