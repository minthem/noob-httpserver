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
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ChainTest {
    @Test
    fun `proceed executes interceptors in order and then the handler`() {
        val events = mutableListOf<String>()
        val context = context()
        val handler: (Context) -> HttpResponse = {
            events += "handler"
            response(HttpStatus.OK)
        }
        val interceptors =
            listOf(
                interceptor {
                    events += "first"
                    it.proceed()
                },
                interceptor {
                    events += "second"
                    it.proceed()
                },
            )

        val actual = Chain(context, interceptors, 0, handler).proceed()

        assertEquals(listOf("first", "second", "handler"), events)
        assertEquals(HttpStatus.OK, actual.status)
    }

    @Test
    fun `proceed passes the same context to every interceptor and handler`() {
        val context = context()
        val seenContexts = mutableListOf<Context>()
        val handler: (Context) -> HttpResponse = {
            seenContexts += it
            response(HttpStatus.OK)
        }
        val interceptors =
            listOf(
                interceptor {
                    seenContexts += it.context
                    it.proceed()
                },
                interceptor {
                    seenContexts += it.context
                    it.proceed()
                },
            )

        Chain(context, interceptors, 0, handler).proceed()

        assertEquals(3, seenContexts.size)
        seenContexts.forEach { assertSame(context, it) }
    }

    @Test
    fun `interceptor can short circuit the chain`() {
        var handlerCalls = 0
        val expected = response(HttpStatus.FORBIDDEN)
        val interceptors =
            listOf(
                interceptor { expected },
            )
        val handler: (Context) -> HttpResponse = {
            handlerCalls++
            response(HttpStatus.OK)
        }

        val actual = Chain(context(), interceptors, 0, handler).proceed()

        assertSame(expected, actual)
        assertEquals(0, handlerCalls)
    }

    @Test
    fun `empty chain invokes the handler directly`() {
        val context = context()
        val expected = response(HttpStatus.NO_CONTENT)
        var received: Context? = null

        val actual =
            Chain(
                context = context,
                interceptors = emptyList(),
                index = 0,
                handler = {
                    received = it
                    expected
                },
            ).proceed()

        assertSame(expected, actual)
        assertSame(context, received)
    }

    @Test
    fun `exception from an interceptor is propagated`() {
        val failure = IllegalStateException("interceptor failed")

        val actual =
            assertFailsWith<IllegalStateException> {
                Chain(
                    context = context(),
                    interceptors = listOf(interceptor { throw failure }),
                    index = 0,
                    handler = { response(HttpStatus.OK) },
                ).proceed()
            }

        assertSame(failure, actual)
    }

    private fun interceptor(block: (Chain) -> HttpResponse): Interceptor =
        object : Interceptor {
            override fun intercept(chain: Chain): HttpResponse = block(chain)
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
