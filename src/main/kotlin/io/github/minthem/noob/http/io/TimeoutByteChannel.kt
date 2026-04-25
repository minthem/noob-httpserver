package io.github.minthem.noob.http.io

import java.nio.ByteBuffer
import java.nio.channels.ByteChannel

internal class TimeoutByteChannel(
    private val delegate: ByteChannel,
    private val timeoutExecutor: TimeoutExecutor,
    private val readTimeoutMillis: Long,
    private val writeTimeoutMillis: Long,
) : ByteChannel {
    override fun isOpen(): Boolean = delegate.isOpen

    override fun close(): Unit = delegate.close()

    override fun write(p0: ByteBuffer?): Int = timeoutExecutor.run(writeTimeoutMillis) { delegate.write(p0) }

    override fun read(p0: ByteBuffer?): Int = timeoutExecutor.run(readTimeoutMillis) { delegate.read(p0) }

    fun read(
        p0: ByteBuffer,
        timeoutMillis: Long,
    ): Int = timeoutExecutor.run(timeoutMillis) { delegate.read(p0) }
}
