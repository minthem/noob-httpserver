package io.github.minthem.noob.http.interceptor

import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpMethod
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.HttpStatus
import io.github.minthem.noob.http.message.RequestTarget
import io.github.minthem.noob.http.router.Context
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class InterceptorRegistryTest {
    @Test
    fun `empty registry invokes the handler`() {
        val expected = response(HttpStatus.OK)
        var calls = 0

        val actual =
            InterceptorRegistry().interceptHandler(context(), handler = {
                calls++
                expected
            })

        assertSame(expected, actual)
        assertEquals(1, calls)
    }

    @Test
    fun `registry preserves interceptor order`() {
        val events = mutableListOf<String>()
        val source =
            mutableListOf(
                interceptor("first", events),
                interceptor("second", events),
            )
        val registry = InterceptorRegistry(source)

        registry.interceptHandler(context()) {
            events += "handler"
            response(HttpStatus.OK)
        }

        assertEquals(listOf("first", "second", "handler"), events)
    }

    @Test
    fun `registry keeps an immutable snapshot of the source list`() {
        val events = mutableListOf<String>()
        val source = mutableListOf(interceptor("original", events))
        val registry = InterceptorRegistry(source)
        source += interceptor("added", events)

        registry.interceptHandler(context()) {
            events += "handler"
            response(HttpStatus.OK)
        }

        assertEquals(listOf("original", "handler"), events)
    }

    private fun interceptor(
        name: String,
        events: MutableList<String>,
    ) =
        object : Interceptor {
            override fun intercept(chain: Chain): HttpResponse {
                events += name
                return chain.proceed()
            }
        }

    private fun context() =
        Context(
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            ),
            emptyMap(),
        )

    private fun response(status: HttpStatus) =
        HttpResponse.build {
            this.status = status
        }
}
