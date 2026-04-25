package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.config.KeepAliveConfig
import io.github.minthem.noob.http.io.ByteReadStream
import io.github.minthem.noob.http.io.TimeoutExecutor
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.HttpResponse
import java.util.concurrent.TimeoutException

internal class KeepAliveManager(
    private val timeoutExecutor: TimeoutExecutor,
    private val config: KeepAliveConfig,
) {
    fun shouldKeepAlive(
        request: HttpRequest,
        response: HttpResponse,
        context: ConnectionContext,
    ): Boolean {
        if (!config.enabled) {
            return false
        }

        if (config.maxRequests.toUInt() < context.reuseCount) {
            return false
        }

        val requestAllowKeepAlive =
            when (request.protocol) {
                HttpProtocol.HTTP_1_0 -> {
                    request.headers["Connection"]?.contains("keep-alive", ignoreCase = true) ?: false
                }

                HttpProtocol.HTTP_1_1 -> {
                    val isClose = request.headers["Connection"]?.contains("close", ignoreCase = true) ?: false
                    !isClose
                }
            }

        val specificClose = response.headers["Connection"]?.contains("close", ignoreCase = true) ?: false

        return requestAllowKeepAlive && !specificClose
    }

    fun waitForNextRequest(stream: ByteReadStream): WaitResult =
        try {
            val next =
                timeoutExecutor.run(config.idleTimeoutMillis) {
                    stream.peak()
                }
            if (next != -1) {
                WaitResult.Ready
            } else {
                WaitResult.Eof
            }
        } catch (_: TimeoutException) {
            WaitResult.Timeout
        } catch (e: Exception) {
            WaitResult.Error(e)
        }
}

internal sealed class WaitResult {
    object Ready : WaitResult()

    object Timeout : WaitResult()

    object Eof : WaitResult()

    data class Error(
        val cause: Throwable,
    ) : WaitResult()
}
