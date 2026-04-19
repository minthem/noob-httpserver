package io.github.minthem.noob.http.server

import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpMethod
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.RequestTarget
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeepAliveStrategyTest {

    private val strategy = KeepAliveStrategy

    @Test
    fun `should keep alive for HTTP 1_1 when request has no connection header and response does not close`() {
        val request = request(HttpProtocol.HTTP_1_1)
        val response = response()

        val actual = strategy.shouldKeepAlive(request, response)

        assertTrue(actual)
    }

    @Test
    fun `should not keep alive for HTTP 1_1 when request contains close`() {
        val request = request(
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.of("connection" to "close")
        )
        val response = response()

        val actual = strategy.shouldKeepAlive(request, response)

        assertFalse(actual)
    }

    @Test
    fun `should not keep alive for HTTP 1_1 when request contains close with different case`() {
        val request = request(
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.of("connection" to "Close")
        )
        val response = response()

        val actual = strategy.shouldKeepAlive(request, response)

        assertFalse(actual)
    }

    @Test
    fun `should not keep alive when response contains close for HTTP 1_1`() {
        val request = request(HttpProtocol.HTTP_1_1)
        val response = response(
            headers = HttpHeaders.of("connection" to "close")
        )

        val actual = strategy.shouldKeepAlive(request, response)

        assertFalse(actual)
    }

    @Test
    fun `should not keep alive for HTTP 1_0 when request has no connection header`() {
        val request = request(HttpProtocol.HTTP_1_0)
        val response = response()

        val actual = strategy.shouldKeepAlive(request, response)

        assertFalse(actual)
    }

    @Test
    fun `should keep alive for HTTP 1_0 when request contains keep alive`() {
        val request = request(
            protocol = HttpProtocol.HTTP_1_0,
            headers = HttpHeaders.of("connection" to "keep-alive")
        )
        val response = response()

        val actual = strategy.shouldKeepAlive(request, response)

        assertTrue(actual)
    }

    @Test
    fun `should keep alive for HTTP 1_0 when request contains keep alive with different case`() {
        val request = request(
            protocol = HttpProtocol.HTTP_1_0,
            headers = HttpHeaders.of("connection" to "Keep-Alive")
        )
        val response = response()

        val actual = strategy.shouldKeepAlive(request, response)

        assertTrue(actual)
    }

    @Test
    fun `should not keep alive for HTTP 1_0 when response contains close`() {
        val request = request(
            protocol = HttpProtocol.HTTP_1_0,
            headers = HttpHeaders.of("connection" to "keep-alive")
        )
        val response = response(
            headers = HttpHeaders.of("connection" to "close")
        )

        val actual = strategy.shouldKeepAlive(request, response)

        assertFalse(actual)
    }

    @Test
    fun `should keep alive when HTTP 1_0 connection header contains multiple values including keep alive`() {
        val request = request(
            protocol = HttpProtocol.HTTP_1_0,
            headers = HttpHeaders.of("connection" to "upgrade, keep-alive")
        )
        val response = response()

        val actual = strategy.shouldKeepAlive(request, response)

        assertTrue(actual)
    }

    @Test
    fun `should not keep alive when HTTP 1_1 request connection header contains close among multiple values`() {
        val request = request(
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.of("connection" to "keep-alive, close")
        )
        val response = response()

        val actual = strategy.shouldKeepAlive(request, response)

        assertFalse(actual)
    }

    private fun request(
        protocol: HttpProtocol,
        headers: HttpHeaders = HttpHeaders.EMPTY
    ): HttpRequest {
        return HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/"),
            protocol = protocol,
            headers = headers,
            bodyStream = ByteArrayInputStream(byteArrayOf())
        )
    }

    private fun response(
        headers: HttpHeaders = HttpHeaders.EMPTY
    ): HttpResponse {
        return HttpResponse.build {
            header(headers)
        }
    }
}