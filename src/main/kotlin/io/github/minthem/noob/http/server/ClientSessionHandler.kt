package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.config.TimeoutConfig
import io.github.minthem.noob.http.exception.HttpResponseException
import io.github.minthem.noob.http.exception.PayloadTooLargeException
import io.github.minthem.noob.http.io.ByteChannelReadStream
import io.github.minthem.noob.http.io.TimeoutByteChannel
import io.github.minthem.noob.http.io.TimeoutExecutor
import io.github.minthem.noob.http.message.FallbackRequestMetadata
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.HttpResponsePreparer
import io.github.minthem.noob.http.message.HttpStatus
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel

internal class ClientSessionHandler(
    private val handler: RequestHandler,
    private val writer: HttpResponseWriter,
    private val keepAliveManager: KeepAliveManager,
    private val timeoutExecutor: TimeoutExecutor,
    private val timeoutConfig: TimeoutConfig,
    private val requestBufferSize: Int,
    private val responsePreparer: HttpResponsePreparer = HttpResponsePreparer(),
) {
    fun handle(context: ConnectionContext) {
        logger.info("--------------- Start new session. ---------------")
        val socket = context.channel
        val buffer = ByteBuffer.allocate(requestBufferSize)
        buffer.flip()
        val channel =
            TimeoutByteChannel(
                context.channel,
                timeoutExecutor,
                timeoutConfig.readMillis,
                timeoutConfig.writeMillis,
            )
        val stream = ByteChannelReadStream(channel, buffer)

        try {
            var isKeepAlive: Boolean
            session@ while (true) {
                isKeepAlive =
                    timeoutExecutor.run(timeoutConfig.sessionMillis) {
                        val result = handler.process(stream)
                        val request = result.request
                        val response = result.response

                        val preparedResponse = responsePreparer.prepare(request, response)

                        writer.write(channel, preparedResponse)

                        keepAliveManager.shouldKeepAlive(
                            request,
                            response,
                            context,
                        )
                    }

                if (!isKeepAlive) {
                    logger.info("Close connection.")
                    break@session
                }

                // KeepAliveで次のパケットが来るまで待機
                when (val waitResult = keepAliveManager.waitForNextRequest(stream)) {
                    WaitResult.Ready -> {
                        logger.info("Keep-Alive session, reuse connection.")
                        context.reuse()
                        continue@session
                    }
                    WaitResult.Eof -> {
                        logger.info("Client closed connection.")
                        break@session
                    }
                    WaitResult.Timeout -> {
                        logger.info("Timeout. Close connection.")
                        break@session
                    }
                    is WaitResult.Error -> {
                        logger.error("Unexpected error: {}", waitResult.cause.message, waitResult.cause)
                        break@session
                    }
                }
            }
        } catch (e: PayloadTooLargeException) {
            logger.error("Payload too large. {}", e.message, e)
            val response =
                HttpResponse.build {
                    status = HttpStatus.PAYLOAD_TOO_LARGE
                    header("Connection", "close")
                    body("Payload too large")
                }
            val prepared = responsePreparer.prepare(FallbackRequestMetadata(), response)
            writer.write(channel, prepared)
        } catch (e: IllegalStateException) {
            if (!socket.isOpen) {
                /**
                 * TODO ソケット切れた際の専用例外を作る (ClientDisconnectedException) read() == -1 とか write() < 0 とかEOFException
                 * 作ったらここでは拾わない
                 */
                logger.error("Connection closed by client. {}", e.message, e)
            } else {
                responseInternalServerError(channel)
            }
        } catch (e: HttpResponseException) {
            if (!socket.isOpen) {
                logger.error("Connection closed by client. {}", e.message, e)
            } else {
                val prepared = responsePreparer.prepare(FallbackRequestMetadata(), e.httpResponse)
                writer.write(channel, prepared)
            }
        } catch (e: Exception) {
            if (socket.isOpen) {
                responseInternalServerError(channel)
            }
            logger.error("Unexpected error during session handling", e)
        } finally {
            logger.info("--------------- End session. ---------------")
        }
    }

    private fun responseInternalServerError(channel: ByteChannel) {
        val response =
            HttpResponse.build {
                status = HttpStatus.INTERNAL_SERVER_ERROR
                header("connection", "close")
            }
        val prepared = responsePreparer.prepare(FallbackRequestMetadata(), response)
        writer.write(channel, prepared)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClientSessionHandler::class.java)
    }
}
