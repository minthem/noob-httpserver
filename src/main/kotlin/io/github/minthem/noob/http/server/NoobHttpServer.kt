package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.config.ServerConfig
import io.github.minthem.noob.http.io.TimeoutExecutor
import io.github.minthem.noob.http.lifecycle.LifecycleEvent
import io.github.minthem.noob.http.lifecycle.LifecycleManager
import io.github.minthem.noob.http.parser.HttpHeadersParser
import io.github.minthem.noob.http.parser.HttpRequestParser
import io.github.minthem.noob.http.router.Router
import io.github.minthem.noob.http.router.RouterRegistry
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.StandardProtocolFamily
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.use

class NoobHttpServer(
    private val config: ServerConfig = ServerConfig()
) {

    private val routerRegistry = RouterRegistry()
    private val lifecycleManager = LifecycleManager()
    private val timeoutExecutor = TimeoutExecutor(Executors.newSingleThreadScheduledExecutor())
    private val executor = Executors.newVirtualThreadPerTaskExecutor()

    @OptIn(ExperimentalAtomicApi::class)
    private val serverIsRunning = AtomicBoolean(false)
    private val serverChannel by lazy {
        val socket = ServerSocketChannel.open(StandardProtocolFamily.INET)
        socket.bind(InetSocketAddress(config.port.toInt()))
        socket
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun start() {
        if (!serverIsRunning.compareAndSet(expectedValue = false, newValue = true)) {
            throw IllegalStateException("Server is already running")
        }

        try {
            val sessionHandler = createSessionHandler()
            val serverEvent = ServerLifecycleEvent(this)

            lifecycleManager.register(serverEvent)
            lifecycleManager.startAll()

            Runtime.getRuntime().addShutdownHook(Thread {
                lifecycleManager.stopAll()
            })

            serverChannel.use { serverChannel ->
                logger.info("Listening on port {}", config.port)

                executor.use { executor ->
                    while (true) {
                        try {
                            val clientSocket = serverChannel.accept()

                            executor.submit {
                                clientSocket.use { socket ->
                                    val context = ConnectionContext.create(socket)
                                    sessionHandler.handle(context)
                                }
                            }
                        } catch (e: Exception) {
                            logger.error("Error during client connection", e)
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            serverIsRunning.store(false)
            throw e
        }
    }

    fun addRouter(router: Router) {
        routerRegistry.register(router)
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun shutdown() {
        try {
            serverChannel.close()
        } catch (e: Exception) {
            logger.error("Error closing server channel", e)
        }
        try {
            executor.shutdown()
            executor.awaitTermination(config.timeouts.shutdownMillis, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error("Interrupted while waiting for executor shutdown", e)
        }

        executor.shutdownNow()
        serverIsRunning.store(false)
    }

    private fun createSessionHandler(): ClientSessionHandler {
        val routeResolver = RouteResolver(routerRegistry)
        val headerParser = HttpHeadersParser(config.httpLimits)
        val requestParser = HttpRequestParser(headerParser, config.httpLimits)
        val requestHandler = RequestHandler(requestParser, routeResolver)
        val writer = HttpResponseWriter(config.buffers.responseHeaderBytes)
        val keepAliveManager = KeepAliveManager(timeoutExecutor, config.keepAlive)

        return ClientSessionHandler(
            handler = requestHandler,
            writer = writer,
            keepAliveManager = keepAliveManager,
            timeoutExecutor = timeoutExecutor,
            timeoutConfig = config.timeouts,
            requestBufferSize = config.buffers.requestBytes,
        )
    }

    private class ServerLifecycleEvent(
        private val server: NoobHttpServer,
    ) : LifecycleEvent {
        override fun onStart() {
            logger.info("Server started")
        }

        override fun onStop() {
            server.shutdown()
            logger.info("Server stopped")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NoobHttpServer::class.java)
    }
}