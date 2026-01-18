package io.github.minthem.noobhttpserver.io

import io.github.minthem.noobhttpserver.http.BodySource
import java.io.InputStream

internal class BodySourceInputStream(private val source: BodySource) : InputStream() {
    override fun read(): Int {
        val b = ByteArray(1)
        val n = source.read(b, 0, 1)
        return if (n == -1) {
            -1
        } else {
            b[0].toInt() and 0xff
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        return source.read(b, off, len)
    }
}
