package io.github.minthem.noob.http.codec

import java.io.InputStream
import java.io.OutputStream

class NativeCodec : StreamCodec {
    override val id: String = "identity"

    override fun encode(output: OutputStream): OutputStream = output

    override fun decode(input: InputStream): InputStream = input
}
