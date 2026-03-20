package io.github.minthem.noobhttpserver.http

import java.net.InetSocketAddress
import java.nio.channels.ByteChannel
import java.nio.channels.SocketChannel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class ConnectionContext(
    val id: String,
    private var _reuseCount: UInt = 0u,
    val createdAt: Instant,
    val remoteIp: String?,
    val remotePort: Int?,
    val channel: ByteChannel
) {

    fun reuse() {
        this._reuseCount++
    }

    val reuseCount: UInt get() = _reuseCount

    val isReused: Boolean get() = _reuseCount > 0u

    companion object {
        @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
        fun create(socket: SocketChannel): ConnectionContext {
            val remoteAddress = socket.remoteAddress as InetSocketAddress?
            val remoteIp = remoteAddress?.address?.hostAddress
            val remotePort = remoteAddress?.port

            val createdAt = Clock.System.now()
            val id = Uuid.generateV7NonMonotonicAt(createdAt).toHexDashString()

            return ConnectionContext(
                id = id,
                createdAt = createdAt,
                remoteIp = remoteIp,
                remotePort = remotePort,
                channel = socket
            )
        }
    }
}
