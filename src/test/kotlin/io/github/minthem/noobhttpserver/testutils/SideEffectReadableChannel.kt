package io.github.minthem.noobhttpserver.testutils

import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel

class SideEffectReadableChannel(
    private val sideEffect: (b: ByteBuffer?) -> Int
) : ReadableByteChannel {
    override fun read(p0: ByteBuffer?): Int = sideEffect(p0)

    override fun isOpen(): Boolean = true

    override fun close() = Unit
}