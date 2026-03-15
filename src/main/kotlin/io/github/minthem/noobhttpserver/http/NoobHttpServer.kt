package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.config.ServerConfig
import io.github.minthem.noobhttpserver.io.TimeoutExecutor
import io.github.minthem.noobhttpserver.router.Router
import io.github.minthem.noobhttpserver.router.RouterRegistry
import java.net.InetSocketAddress
import java.net.StandardProtocolFamily
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.Executors

class NoobHttpServer(
    private val config: ServerConfig = ServerConfig()
) {

    private val routerRegistry = RouterRegistry()
    private val timeoutExecutor = TimeoutExecutor(Executors.newSingleThreadScheduledExecutor())

    fun start() {
        val sessionHandler = createSessionHandler()
        val addr = InetSocketAddress(config.port.toInt())

        try {
            ServerSocketChannel.open(StandardProtocolFamily.INET).bind(addr).use { serverChannel ->
                println("Listening on port ${config.port}")

                Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                    while (true) {
                        try {
                            val clientSocket = serverChannel.accept()
                            println(clientSocket.remoteAddress)

                            executor.submit {
                                clientSocket.use { socket ->
                                    sessionHandler.handle(socket)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addRouter(router: Router) {
        routerRegistry.register(router)
    }

    private fun createSessionHandler(): ClientSessionHandler {
        val routeResolver = RouteResolver(routerRegistry)
        val headerParser = HttpHeadersParser(config.httpLimits)
        val requestParser = HttpRequestParser(headerParser, config.httpLimits)
        val requestHandler = RequestHandler(requestParser, routeResolver)
        val writer = HttpResponseWriter(config.buffers.responseHeaderBytes)
        val keepAliveStrategy = KeepAliveStrategy

        return ClientSessionHandler(
            handler = requestHandler,
            writer = writer,
            keepAliveStrategy = keepAliveStrategy,
            timeoutExecutor = timeoutExecutor,
            timeoutConfig = config.timeouts,
            requestBufferSize = config.buffers.requestBytes,
        )
    }
}