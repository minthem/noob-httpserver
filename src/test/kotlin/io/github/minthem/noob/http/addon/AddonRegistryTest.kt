package io.github.minthem.noob.http.addon

import io.github.minthem.noob.http.exception.HttpResponseException
import io.github.minthem.noob.http.message.BodyEncoding
import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpMethod
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.HttpResponse
import io.github.minthem.noob.http.message.HttpStatus
import io.github.minthem.noob.http.message.RequestTarget
import io.github.minthem.noob.http.router.Context
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddonRegistryTest {
    @Test
    fun `should intercept request in registration order and complete in reverse order`() {
        val events = mutableListOf<String>()
        val registry =
            AddonRegistry(
                listOf(
                    interceptorAddon("A", events),
                    interceptorAddon("B", events),
                ),
            )

        requestContext().use { context ->
            registry.interceptRequest(context) {
                events.add("handler")
                HttpResponse.build { }
            }
        }

        assertEquals(listOf("A-before", "B-before", "handler", "B-after", "A-after"), events)
    }

    @Test
    fun `should allow interceptor to replace response`() {
        var handlerCalled = false
        val registry =
            AddonRegistry(
                listOf(
                    ServerAddon { registrar ->
                        registrar.interceptRequests { _, next ->
                            next()
                            HttpResponse.build { status = HttpStatus.CREATED }
                        }
                    },
                ),
            )

        val response =
            requestContext().use { context ->
                registry.interceptRequest(context) {
                    handlerCalled = true
                    HttpResponse.build { status = HttpStatus.OK }
                }
            }

        assertTrue(handlerCalled)
        assertEquals(HttpStatus.CREATED, response.status)
    }

    @Test
    fun `should allow interceptor to short circuit request`() {
        var handlerCalled = false
        val registry =
            AddonRegistry(
                listOf(
                    ServerAddon { registrar ->
                        registrar.interceptRequests { _, _ ->
                            HttpResponse.build { status = HttpStatus.ACCEPTED }
                        }
                    },
                ),
            )

        val response =
            requestContext().use { context ->
                registry.interceptRequest(context) {
                    handlerCalled = true
                    HttpResponse.build { }
                }
            }

        assertFalse(handlerCalled)
        assertEquals(HttpStatus.ACCEPTED, response.status)
    }

    @Test
    fun `should execute interceptor finally when handler throws HttpResponseException`() {
        var cleanedUp = false
        val registry = cleanupRegistry { cleanedUp = true }

        assertFailsWith<HttpResponseException> {
            requestContext().use { context ->
                registry.interceptRequest(context) {
                    throw HttpResponseException(httpResponse = HttpResponse.build { status = HttpStatus.BAD_REQUEST })
                }
            }
        }

        assertTrue(cleanedUp)
    }

    @Test
    fun `should execute interceptor finally when handler throws unexpected exception`() {
        var cleanedUp = false
        val registry = cleanupRegistry { cleanedUp = true }

        assertFailsWith<IllegalStateException> {
            requestContext().use { context ->
                registry.interceptRequest(context) {
                    throw IllegalStateException("unexpected")
                }
            }
        }

        assertTrue(cleanedUp)
    }

    @Test
    fun `should reject duplicate body encoding registrations`() {
        val duplicateRegistration =
            ServerAddon { registrar ->
                repeat(2) {
                    registrar.registerBodyEncoding(
                        encoding = BodyEncoding.GZIP,
                        preservesContentLength = false,
                        decoder = { it },
                        encoder = { it },
                    )
                }
            }

        assertFailsWith<IllegalArgumentException> {
            AddonRegistry(listOf(duplicateRegistration))
        }
    }

    private fun interceptorAddon(
        name: String,
        events: MutableList<String>,
    ): ServerAddon =
        ServerAddon { registrar ->
            registrar.interceptRequests { _, next ->
                events.add("$name-before")
                try {
                    next()
                } finally {
                    events.add("$name-after")
                }
            }
        }

    private fun cleanupRegistry(cleanup: () -> Unit): AddonRegistry =
        AddonRegistry(
            listOf(
                ServerAddon { registrar ->
                    registrar.interceptRequests { _, next ->
                        try {
                            next()
                        } finally {
                            cleanup()
                        }
                    }
                },
            ),
        )

    private fun requestContext(): Context =
        Context(
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = InputStream.nullInputStream(),
            ),
            emptyMap(),
        )
}
