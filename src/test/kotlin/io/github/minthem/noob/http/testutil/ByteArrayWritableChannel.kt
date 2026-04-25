package io.github.minthem.noob.http.testutil

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel

class ByteArrayWritableChannel(
    private val stream: ByteArrayOutputStream = ByteArrayOutputStream(),
    private val bufferSize: Int = 512,
) : WritableByteChannel {
    override fun write(p0: ByteBuffer?): Int {
        p0 ?: return 0
        val toWrite = minOf(p0.remaining(), bufferSize)
        val array = ByteArray(toWrite)

        p0.get(array)
        stream.write(array)

        return toWrite
    }

    override fun isOpen(): Boolean = true

    override fun close() = stream.close()

    fun toByteArray(): ByteArray = stream.toByteArray()
}
