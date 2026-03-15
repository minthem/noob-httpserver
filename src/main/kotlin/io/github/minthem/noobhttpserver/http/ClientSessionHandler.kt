package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.exception.HttpResponseException
import io.github.minthem.noobhttpserver.io.ByteChannelReadStream
import io.github.minthem.noobhttpserver.io.TimeoutByteChannel
import io.github.minthem.noobhttpserver.io.TimeoutExecutor
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel

// TODO logger
internal class ClientSessionHandler(
    private val handler: RequestHandler,
    private val writer: HttpResponseWriter,
    private val keepAliveStrategy: KeepAliveStrategy,
    private val timeoutExecutor: TimeoutExecutor
) {
    fun handle(socket: ByteChannel) {
        println("--------------- Start new session. ---------------")
        val buffer = ByteBuffer.allocate(8192)
        buffer.flip()
        val channel = TimeoutByteChannel(
            socket, timeoutExecutor,
            30000, // TODO Parameterize
            30000, // TODO Parameterize
        )
        val stream = ByteChannelReadStream(channel, buffer)

        try {
            var isKeepAlive: Boolean
            session@while (true) {
                isKeepAlive = timeoutExecutor.run(120000) { // TODO Parameterize
                    val result = handler.process(stream)
                    val request = result.request
                    val response = result.response

                    writer.write(channel, request.protocol, response)

                    keepAliveStrategy.shouldKeepAlive(
                        request, response
                    )
                }

                if(isKeepAlive) {
                    println("Keep-Alive session, reuse connection.")
                } else {
                    println("Close session.")
                    break@session
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