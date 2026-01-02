package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.exception.HttpResponseException
import io.github.minthem.noobhttpserver.exception.MethodNotAllowException
import io.github.minthem.noobhttpserver.exception.RouteNotFoundException
import io.github.minthem.noobhttpserver.router.Context
import io.github.minthem.noobhttpserver.router.RouteMatchResult
import io.github.minthem.noobhttpserver.router.Router
import io.github.minthem.noobhttpserver.router.RouterRegistry
import java.net.InetSocketAddress
import java.net.StandardProtocolFamily
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

class Server(
    private val port: UShort
) {

    private val routerRegistry = RouterRegistry()

    fun start() {
        val addr = InetSocketAddress(port.toInt())
        try {
            ServerSocketChannel.open(StandardProtocolFamily.INET).bind(addr).use { channel ->
                println("Listening on port $port")
                Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                    while (true) {
                        try {
                            val clientSocket = channel.accept()
                            println(clientSocket.remoteAddress)
                            executor.submit {
                                clientSocket.use { socket ->
                                    println("--------------- Start new session. ---------------")
                                    var isKeepAlive = false
                                    try {
                                        val buffer = ByteBuffer.allocate(8192)
                                        buffer.flip()
                                        while (true) {
                                            val request = HttpRequestParser().parse(socket, buffer)

                                            println(request.headers)
                                            println(request.method)
                                            println(request.path)
                                            println(request.protocol)

                                            val match = findHandler(request)
                                            val context = Context(request, match.pathParams)
                                            val response = match.handler.invoke(context)

                                            isKeepAlive = isKeepAlive(
                                                request.protocol,
                                                request.headers
                                            ) && !(response.headers["Connection"]?.contains("close") ?: false)

                                            writeResponse(socket, request.protocol, response)

                                            if (!isKeepAlive) break

                                            println("Keep-Alive session, reuse connection.")
                                        }
                                    } catch (e: IllegalStateException) {
                                        println("Connection closed by client. $e")
                                        return@submit
                                    } catch (e: HttpResponseException) {
                                        if (socket.isOpen) {
                                            writeResponse(socket, HttpProtocol.HTTP_1_1, e.httpResponse)
                                        }
                                        return@submit
                                    } catch (e: Exception) {
                                        if (socket.isOpen) {
                                            val response = HttpResponse.build {
                                                status = HttpStatus.INTERNAL_SERVER_ERROR
                                                header("connection", "close")
                                            }
                                            writeResponse(socket, HttpProtocol.HTTP_1_1, response)
                                        }
                                        e.printStackTrace()
                                        return@submit
                                    }
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
            return
        }
    }

    fun addRouter(router: Router) {
        routerRegistry.register(router)
    }

    private fun findHandler(request: HttpRequest): RouteMatchResult.Match {
        when (val matchResult = routerRegistry.find(request)) {
            is RouteMatchResult.Match -> {
                return matchResult
            }

            is RouteMatchResult.MethodNotAllowed -> {
                throw MethodNotAllowException(request.method, matchResult.allowedMethods)
            }

            is RouteMatchResult.NotFound -> {
                throw RouteNotFoundException(request.method, request.path)
            }
        }
    }

    // FIXME 無理やり動かしているため、後で直す
    private fun writeResponse(socket: SocketChannel, protocol: HttpProtocol, response: HttpResponse) {
        val buffer = ByteBuffer.allocate(2048)
        val statusLine =
            "${protocol.version()} ${response.status.code} ${response.status.reasonPhrase}\r\n".toByteArray(Charsets.US_ASCII)

        buffer.put(statusLine)
        response.headers.forEach { key, values ->
            values.forEach { value ->
                val writeLine = "$key: $value\r\n".toByteArray(Charsets.US_ASCII)
                // FIXME ヘッダ長がバッファより長いと破綻するため、要調整
                if (buffer.remaining() < writeLine.size) {
                    buffer.flip()
                    val written = socket.write(buffer)
                    if (written < 0) throw IllegalStateException("Unexpected end of stream")
                    buffer.compact()
                }
            }
        }

        if ("content-type" !in response.headers && response.body.defaultContentType() != null) {
            val writeLine = "Content-Type: ${response.body.defaultContentType()}\r\n".toByteArray(Charsets.US_ASCII)
            buffer.put(writeLine)
        }

        if ("content-length" !in response.headers) {
            val contentLength = response.body.contentLength()
            val writeLine = "Content-Length: $contentLength\r\n".toByteArray(Charsets.US_ASCII)
            buffer.put(writeLine)
        }

        if ("date" !in response.headers) {
            val writeLine = "Date: ${
                ZonedDateTime.now(ZoneId.of("UTC")).format(FIELD_DATE_FORMATTER)
            }\r\n".toByteArray(Charsets.US_ASCII)
            buffer.put(writeLine)
        }

        buffer.put("\r\n".toByteArray(Charsets.US_ASCII))
        while (buffer.hasRemaining()) {
            buffer.flip()
            val written = socket.write(buffer)
            if (written < 0) throw IllegalStateException("Unexpected end of stream")
//            buffer.compact()
        }

        response.body.writeTo(socket)
    }

    private fun isKeepAlive(protocol: HttpProtocol, headers: HttpHeaders): Boolean {
        return when (protocol) {
            HttpProtocol.HTTP_1_1 -> !(headers["Connection"]?.contains("close") ?: false)
            HttpProtocol.HTTP_1_0 -> headers["Connection"]?.contains("keep-alive") == true
        }
    }

    companion object {
        private val FIELD_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME
    }
}