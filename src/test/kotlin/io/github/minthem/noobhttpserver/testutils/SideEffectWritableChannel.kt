package io.github.minthem.noobhttpserver.testutils

import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

class SideEffectWritableChannel(
    private val sideEffect: (b: ByteBuffer?) -> Int
): WritableByteChannel {
    override fun write(p0: ByteBuffer?): Int = sideEffect(p0)

    override fun isOpen(): Boolean = true

    override fun close() = Unit
}
