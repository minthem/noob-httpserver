package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.config.ServerConfig
import io.github.minthem.noobhttpserver.io.TimeoutExecutor
import io.github.minthem.noobhttpserver.lifecycle.LifecycleEvent
import io.github.minthem.noobhttpserver.lifecycle.LifecycleManager
import io.github.minthem.noobhttpserver.router.Router
import io.github.minthem.noobhttpserver.router.RouterRegistry
import java.net.InetSocketAddress
import java.net.StandardProtocolFamily
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

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
                println("Listening on port ${config.port}")

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
                            e.printStackTrace()
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
            e.printStackTrace()
        }
        try {
            executor.shutdown()
            executor.awaitTermination(config.timeouts.shutdownMillis, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            e.printStackTrace()
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
            println("Server started")
        }

        override fun onStop() {
            server.shutdown()
            println("Server stopped")
        }
    }
}