package io.github.minthem.noobhttpserver.http.server

import io.github.minthem.noobhttpserver.http.header.HttpHeaders
import io.github.minthem.noobhttpserver.http.request.HttpRequestParser
import java.net.InetSocketAddress
import java.net.StandardProtocolFamily
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.Executors

class Server(
    private val port: UShort
) {

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

                                            isKeepAlive = isKeepAlive(request.protocol, request.headers)

                                            socket.write(ByteBuffer.wrap("HTTP/1.1 200 OK\r\n".toByteArray()))
                                            socket.write(ByteBuffer.wrap("Content-Length: 13\r\n".toByteArray()))
                                            socket.write(ByteBuffer.wrap("Content-Type: text/plain\r\n".toByteArray()))
                                            socket.write(ByteBuffer.wrap("Date: Tue Dec 23 01:50:04 JST 2025\r\n".toByteArray()))
                                            socket.write(ByteBuffer.wrap("Host: localhost\r\n".toByteArray()))
                                            socket.write(ByteBuffer.wrap("Connection: keep-alive\r\n".toByteArray()))
                                            socket.write(ByteBuffer.wrap("\r\n".toByteArray()))
                                            socket.write(ByteBuffer.wrap("Hello World\r\n".toByteArray()))

                                            if (!isKeepAlive) break

                                            println("Keep-Alive session, reuse connection.")
                                        }
                                    } catch (e: IllegalStateException) {
                                        println("Connection closed by client. $e")
                                    } catch (e: Exception) {
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

    private fun isKeepAlive(protocol: String, headers: HttpHeaders): Boolean {
        return when (protocol) {
            "HTTP/1.1" -> !(headers["Connection"]?.contains("close") ?: false)
            "HTTP/1.0" -> headers["Connection"]?.contains("keep-alive") == true
            else -> false
        }
    }
}