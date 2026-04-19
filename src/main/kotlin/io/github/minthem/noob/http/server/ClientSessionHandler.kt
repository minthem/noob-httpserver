package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.config.TimeoutConfig
import io.github.minthem.noob.http.exception.HttpResponseException
import io.github.minthem.noob.http.io.ByteChannelReadStream
import io.github.minthem.noob.http.io.TimeoutByteChannel
import io.github.minthem.noob.http.io.TimeoutExecutor
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.HttpStatus
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel

// TODO logger
internal class ClientSessionHandler(
    private val handler: RequestHandler,
    private val writer: HttpResponseWriter,
    private val keepAliveManager: KeepAliveManager,
    private val timeoutExecutor: TimeoutExecutor,
    private val timeoutConfig: TimeoutConfig,
    private val requestBufferSize: Int,
) {
    fun handle(context: ConnectionContext) {
        println("--------------- Start new session. ---------------")
        val socket = context.channel
        val buffer = ByteBuffer.allocate(requestBufferSize)
        buffer.flip()
        val channel = TimeoutByteChannel(
            context.channel, timeoutExecutor,
            timeoutConfig.readMillis, timeoutConfig.writeMillis
        )
        val stream = ByteChannelReadStream(channel, buffer)

        try {
            var isKeepAlive: Boolean
            session@ while (true) {
                isKeepAlive = timeoutExecutor.run(timeoutConfig.sessionMillis) {
                    val result = handler.process(stream)
                    val request = result.request
                    val response = result.response

                    writer.write(channel, request.protocol, response)

                    keepAliveManager.shouldKeepAlive(
                        request, response, context
                    )
                }

                if (!isKeepAlive) {
                    println("Close connection.")
                    break@session
                }

                // KeepAliveで次のパケットが来るまで待機
                when (val waitResult = keepAliveManager.waitForNextRequest(stream)) {
                    WaitResult.Ready -> {
                        println("Keep-Alive session, reuse connection.")
                        context.reuse()
                        continue@session
                    }
                    WaitResult.Eof -> {
                        println("Client closed connection.")
                        break@session
                    }
                    WaitResult.Timeout -> {
                        println("Timeout. Close connection.")
                        break@session
                    }
                    is WaitResult.Error -> {
                        println("Unexpected error: ${waitResult.cause}")
                        break@session
                    }
                }
            }
        } catch (e: IllegalStateException) {
            if (!socket.isOpen) {
                /**
                 * TODO ソケット切れた際の専用例外を作る (ClientDisconnectedException) read() == -1 とか write() < 0 とかEOFException
                 * 作ったらここでは拾わない
                 */
                println("Connection closed by client. $e")
            } else {
                responseInternalServerError(channel)
            }
        } catch (e: HttpResponseException) {
            if (!socket.isOpen) {
                println("Connection closed by client. $e")
            } else {
                writer.write(channel, HttpProtocol.HTTP_1_1, e.httpResponse)
            }
        } catch (e: Exception) {
            if (socket.isOpen) {
                responseInternalServerError(channel)
            }
            e.printStackTrace()
        } finally {
            println("--------------- End session. ---------------")
        }
    }

    private fun responseInternalServerError(channel: ByteChannel) {
        val response = HttpResponse.build {
            status = HttpStatus.INTERNAL_SERVER_ERROR
            header("connection", "close")
        }
        writer.write(channel, HttpProtocol.HTTP_1_1, response)
    }
}