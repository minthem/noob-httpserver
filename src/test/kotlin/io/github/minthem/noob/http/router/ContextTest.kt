package io.github.minthem.noob.http.router

import io.github.minthem.noob.http.message.HttpHeaders
import io.github.minthem.noob.http.message.HttpMethod
import io.github.minthem.noob.http.message.HttpProtocol
import io.github.minthem.noob.http.message.HttpRequest
import io.github.minthem.noob.http.message.RequestTarget
import io.github.minthem.noob.http.multipart.Multipart
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import kotlin.test.assertFailsWith

internal class ContextTest {
    @Test
    fun `test queryParam returns correct value`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test?key1=value1&key2=value2"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, emptyMap())

        val result = context.queryParam("key1")

        assertEquals("value1", result)
    }

    @Test
    fun `test queryParam returns null if key does not exist`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test?key1=value1"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, mapOf("pkey1" to "pvalue1"))

        val result = context.queryParam("key2")

        assertNull(result)
        assertEquals("pvalue1", context.pathParams["pkey1"])
    }

    @Test
    fun `pathParams are copied when the context is created`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val pathParams = mutableMapOf("id" to "before")
        val context = Context(request, pathParams)

        pathParams["id"] = "after"
        pathParams["new"] = "value"

        assertEquals(mapOf("id" to "before"), context.pathParams)
    }

    @Test
    fun `test queryParam returns first value when multiple values exist`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test?key=a&key=b"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, emptyMap())

        assertEquals("a", context.queryParam("key"))
    }

    @Test
    fun `test queryParamAs converts to correct type`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test?intKey=42&boolKey=true"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, emptyMap())

        val intValue: Int? = context.queryParamAs("intKey")
        val boolValue: Boolean? = context.queryParamAs("boolKey")

        assertEquals(42, intValue)
        assertTrue(boolValue!!)
    }

    @Test
    fun `test queryParamAs returns null for missing key`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, emptyMap())

        val result: Int? = context.queryParamAs("missingKey")

        assertNull(result)
    }

    @Test
    fun `test queryParamAs returns null for unsupported type`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test?key=someValue"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, emptyMap())

        assertFailsWith<IllegalArgumentException> {
            context.queryParamAs<Map<String, String>>("key")
        }
    }

    @Test
    fun `test queryParamAs with default returns correct value`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test?key=123"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, emptyMap())

        val result: Int = context.queryParamAs("key", 0)

        assertEquals(123, result)
    }

    @Test
    fun `test queryParamAs with default returns default value if key does not exist`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, emptyMap())

        val result: Int = context.queryParamAs("missingKey", 99)

        assertEquals(99, result)
    }

    @Test
    fun `test path and query params should be decoded`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/hello%20world?name=%E3%81%82&message=hello+world"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, emptyMap())

        assertEquals("/hello world", context.path)
        assertEquals("あ", context.queryParam("name"))
        assertEquals("hello world", context.queryParam("message"))
    }

    @Test
    fun `test bodyAsText reads text correctly`() {
        val bodyContent = "Sample body content"
        val request =
            HttpRequest(
                method = HttpMethod.POST,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(bodyContent.toByteArray(Charset.defaultCharset())),
            )
        val context = Context(request, emptyMap())

        val result = context.bodyAsText()

        assertEquals(bodyContent, result)
    }

    @Test
    fun `test bodyAsBytes reads bytes correctly`() {
        val bodyContent = "Another body content"
        val request =
            HttpRequest(
                method = HttpMethod.POST,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(bodyContent.toByteArray(Charset.defaultCharset())),
            )
        val context = Context(request, emptyMap())

        val result = context.bodyAsBytes()

        assertArrayEquals(bodyContent.toByteArray(Charset.defaultCharset()), result)
    }

    @Test
    fun `test bodyAsText throws if body stream already read by bytes`() {
        val request =
            HttpRequest(
                method = HttpMethod.POST,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream("hello".toByteArray()),
            )
        val context = Context(request, emptyMap())

        context.bodyAsBytes()

        val exception =
            assertFailsWith<IllegalStateException> {
                context.bodyAsText()
            }
        assertEquals("Body stream has already been read", exception.message)
    }

    @Test
    fun `test bodyAsBytes throws if body stream already read by text`() {
        val request =
            HttpRequest(
                method = HttpMethod.POST,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream("hello".toByteArray()),
            )
        val context = Context(request, emptyMap())

        context.bodyAsText()

        val exception =
            assertFailsWith<IllegalStateException> {
                context.bodyAsBytes()
            }
        assertEquals("Body stream has already been read", exception.message)
    }

    @Test
    fun `test deferred actions should run in reverse order`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, emptyMap())
        val events = mutableListOf<String>()

        context.defer { events.add("first") }
        context.defer { events.add("second") }
        context.defer { events.add("third") }

        context.close()

        assertEquals(listOf("third", "second", "first"), events)
    }

    @Test
    fun `test close should continue executing deferred actions after exception`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, emptyMap())
        val events = mutableListOf<String>()

        context.defer {
            events.add("first")
            throw IllegalStateException("boom")
        }
        context.defer {
            events.add("second")
        }

        context.close()

        assertEquals(listOf("second", "first"), events)
    }

    @Test
    fun `test close should be safe to call multiple times`() {
        val request =
            HttpRequest(
                method = HttpMethod.GET,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = HttpHeaders.EMPTY,
                bodyStream = ByteArrayInputStream(ByteArray(0)),
            )
        val context = Context(request, emptyMap())

        context.close()
        context.close()
    }

    @Nested
    @DisplayName("Multipart Body Parsing Tests")
    inner class MultipartBodyParsingTests {
        @Test
        fun `test bodyAsMultipart parses valid multipart body`() {
            val boundary = "----BoundaryXYZ"
            val bodyContent =
                """
                ------BoundaryXYZ
                Content-Disposition: form-data; name="field1"

                value1
                ------BoundaryXYZ
                Content-Disposition: form-data; name="field2"; filename="file.txt"
                Content-Type: text/plain

                file content here
                ------BoundaryXYZ--
                """.trimIndent().replace("\n", "\r\n")
            val headers =
                HttpHeaders.of(
                    "Content-Type" to "multipart/form-data; boundary=$boundary",
                )
            val request =
                HttpRequest(
                    method = HttpMethod.POST,
                    path = RequestTarget("/test"),
                    protocol = HttpProtocol.HTTP_1_1,
                    headers = headers,
                    bodyStream = ByteArrayInputStream(bodyContent.toByteArray(Charset.defaultCharset())),
                )
            val context = Context(request, emptyMap())

            val multipartBody = context.bodyAsMultipart()
            val field1 = multipartBody.part("field1")
            val field2 = multipartBody.part("field2")

            assertNotNull(field1)
            assertEquals("value1", (field1 as Multipart.FormField).value)
            assertNotNull(field2)
            assertEquals("file content here", (field2 as Multipart.FileUpload).asStream().reader().readText())
            assertEquals("file.txt", field2.filename)
        }

        @Test
        fun `test bodyAsMultipart throws exception if boundary is missing`() {
            val headers =
                HttpHeaders.of(
                    "Content-Type" to "multipart/form-data",
                )
            val request =
                HttpRequest(
                    method = HttpMethod.POST,
                    path = RequestTarget("/test"),
                    protocol = HttpProtocol.HTTP_1_1,
                    headers = headers,
                    bodyStream = ByteArrayInputStream(ByteArray(0)),
                )
            val context = Context(request, emptyMap())

            val exception =
                assertFailsWith<IllegalStateException> {
                    context.bodyAsMultipart()
                }
            assertEquals("Missing boundary parameter", exception.message)
        }

        @Test
        fun `test bodyAsMultipart throws exception for invalid Content-Type`() {
            val headers =
                HttpHeaders.of(
                    "Content-Type" to "application/json",
                )
            val request =
                HttpRequest(
                    method = HttpMethod.POST,
                    path = RequestTarget("/test"),
                    protocol = HttpProtocol.HTTP_1_1,
                    headers = headers,
                    bodyStream = ByteArrayInputStream(ByteArray(0)),
                )
            val context = Context(request, emptyMap())

            val exception =
                assertFailsWith<IllegalStateException> {
                    context.bodyAsMultipart()
                }
            assertEquals("Content-Type must be multipart/form-data", exception.message)
        }

        @Test
        fun `test bodyAsMultipart throws exception when content type is missing`() {
            val request =
                HttpRequest(
                    method = HttpMethod.POST,
                    path = RequestTarget("/test"),
                    protocol = HttpProtocol.HTTP_1_1,
                    headers = HttpHeaders.EMPTY,
                    bodyStream = ByteArrayInputStream(ByteArray(0)),
                )
            val context = Context(request, emptyMap())

            val exception =
                assertFailsWith<IllegalStateException> {
                    context.bodyAsMultipart()
                }
            assertEquals("Content-Type must be multipart/form-data", exception.message)
        }

        @Test
        fun `test bodyAsMultipart throws exception if body stream already read`() {
            val boundary = "----BoundaryXYZ"
            val bodyContent =
                """
                ------BoundaryXYZ
                Content-Disposition: form-data; name="field1"

                value1
                ------BoundaryXYZ--
                """.trimIndent().replace("\n", "\r\n")
            val headers =
                HttpHeaders.of(
                    "Content-Type" to "multipart/form-data; boundary=$boundary",
                )
            val request =
                HttpRequest(
                    method = HttpMethod.POST,
                    path = RequestTarget("/test"),
                    protocol = HttpProtocol.HTTP_1_1,
                    headers = headers,
                    bodyStream = ByteArrayInputStream(bodyContent.toByteArray(Charset.defaultCharset())),
                )
            val context = Context(request, emptyMap())

            context.bodyAsText()

            val exception =
                assertFailsWith<IllegalStateException> {
                    context.bodyAsMultipart()
                }
            assertEquals("Body stream has already been read for multipart parsing", exception.message)
        }

        @Test
        fun `test bodyAsText throws if body stream already read by multipart`() {
            val boundary = "----BoundaryXYZ"
            val bodyContent =
                """
                ------BoundaryXYZ
                Content-Disposition: form-data; name="field1"

                value1
                ------BoundaryXYZ--
                """.trimIndent().replace("\n", "\r\n")
            val headers =
                HttpHeaders.of(
                    "Content-Type" to "multipart/form-data; boundary=$boundary",
                )
            val request =
                HttpRequest(
                    method = HttpMethod.POST,
                    path = RequestTarget("/test"),
                    protocol = HttpProtocol.HTTP_1_1,
                    headers = headers,
                    bodyStream = ByteArrayInputStream(bodyContent.toByteArray(Charset.defaultCharset())),
                )
            val context = Context(request, emptyMap())

            context.bodyAsMultipart()

            val exception =
                assertFailsWith<IllegalStateException> {
                    context.bodyAsText()
                }
            assertEquals("Body stream has already been read", exception.message)
        }

        @Test
        fun `test bodyAsBytes throws if body stream already read by multipart`() {
            val boundary = "----BoundaryXYZ"
            val bodyContent =
                """
                ------BoundaryXYZ
                Content-Disposition: form-data; name="field1"

                value1
                ------BoundaryXYZ--
                """.trimIndent().replace("\n", "\r\n")
            val headers =
                HttpHeaders.of(
                    "Content-Type" to "multipart/form-data; boundary=$boundary",
                )
            val request =
                HttpRequest(
                    method = HttpMethod.POST,
                    path = RequestTarget("/test"),
                    protocol = HttpProtocol.HTTP_1_1,
                    headers = headers,
                    bodyStream = ByteArrayInputStream(bodyContent.toByteArray(Charset.defaultCharset())),
                )
            val context = Context(request, emptyMap())

            context.bodyAsMultipart()

            val exception =
                assertFailsWith<IllegalStateException> {
                    context.bodyAsBytes()
                }
            assertEquals("Body stream has already been read", exception.message)
        }
    }
}
