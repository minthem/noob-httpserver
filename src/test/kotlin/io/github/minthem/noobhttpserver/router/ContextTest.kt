package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.HttpHeaders
import io.github.minthem.noobhttpserver.http.HttpRequest
import io.github.minthem.noobhttpserver.http.HttpMethod
import io.github.minthem.noobhttpserver.http.HttpProtocol
import io.github.minthem.noobhttpserver.http.Multipart
import io.github.minthem.noobhttpserver.http.RequestTarget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import kotlin.test.assertFailsWith

internal class ContextTest {

    /**
     * Test for retrieving query parameters as String.
     */
    @Test
    fun `test queryParam returns correct value`() {
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/test?key1=value1&key2=value2"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = ByteArrayInputStream(ByteArray(0))
        )
        val context = Context(request, emptyMap())

        val result = context.queryParam("key1")

        assertEquals("value1", result)
    }

    /**
     * Test for retrieving non-existent query parameters as null.
     */
    @Test
    fun `test queryParam returns null if key does not exist`() {
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/test?key1=value1"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = ByteArrayInputStream(ByteArray(0))
        )
        val context = Context(request, emptyMap())

        val result = context.queryParam("key2")

        assertNull(result)
    }

    /**
     * Test for converting query parameter to a specific type.
     */
    @Test
    fun `test queryParamAs converts to correct type`() {
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/test?intKey=42&boolKey=true"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = ByteArrayInputStream(ByteArray(0))
        )
        val context = Context(request, emptyMap())

        val intValue: Int? = context.queryParamAs("intKey")
        val boolValue: Boolean? = context.queryParamAs("boolKey")

        assertEquals(42, intValue)
        assertTrue(boolValue!!)
    }

    /**
     * Test for queryParamAs with unsupported type.
     */
    @Test
    fun `test queryParamAs returns null for unsupported type`() {
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/test?key=someValue"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = ByteArrayInputStream(ByteArray(0))
        )
        val context = Context(request, emptyMap())

        assertFailsWith<IllegalArgumentException> { context.queryParamAs<Map<String, String>>("key") }
    }

    /**
     * Test for queryParamAs with the default value.
     */
    @Test
    fun `test queryParamAs with default returns correct value`() {
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/test?key=123"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = ByteArrayInputStream(ByteArray(0))
        )
        val context = Context(request, emptyMap())

        val result: Int = context.queryParamAs("key", 0)

        assertEquals(123, result)
    }

    /**
     * Test for queryParamAs with default value when key does not exist.
     */
    @Test
    fun `test queryParamAs with default returns default value if key does not exist`() {
        val request = HttpRequest(
            method = HttpMethod.GET,
            path = RequestTarget("/test"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = ByteArrayInputStream(ByteArray(0))
        )
        val context = Context(request, emptyMap())

        val result: Int = context.queryParamAs("missingKey", 99)

        assertEquals(99, result)
    }

    /**
     * Test for reading body as text.
     */
    @Test
    fun `test bodyAsText reads text correctly`() {
        val bodyContent = "Sample body content"
        val request = HttpRequest(
            method = HttpMethod.POST,
            path = RequestTarget("/test"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = ByteArrayInputStream(bodyContent.toByteArray(Charset.defaultCharset()))
        )
        val context = Context(request, emptyMap())

        val result = context.bodyAsText()

        assertEquals(bodyContent, result)
    }

    /**
     * Test for reading body as bytes.
     */
    @Test
    fun `test bodyAsBytes reads bytes correctly`() {
        val bodyContent = "Another body content"
        val request = HttpRequest(
            method = HttpMethod.POST,
            path = RequestTarget("/test"),
            protocol = HttpProtocol.HTTP_1_1,
            headers = HttpHeaders.EMPTY,
            bodyStream = ByteArrayInputStream(bodyContent.toByteArray(Charset.defaultCharset()))
        )
        val context = Context(request, emptyMap())

        val result = context.bodyAsBytes()

        assertArrayEquals(bodyContent.toByteArray(Charset.defaultCharset()), result)
    }

    @Nested
    @DisplayName("Multipart Body Parsing Tests")
    inner class MultipartBodyParsingTests {

        /**
         * Test for parsing a valid multipart body.
         */
        @Test
        fun `test bodyAsMultipart parses valid multipart body`() {
            val boundary = "----BoundaryXYZ"
            val bodyContent = """
            ------BoundaryXYZ
            Content-Disposition: form-data; name="field1"

            value1
            ------BoundaryXYZ
            Content-Disposition: form-data; name="field2"; filename="file.txt"
            Content-Type: text/plain

            file content here
            ------BoundaryXYZ--
        """.trimIndent().replace("\n", "\r\n")
            val headers = HttpHeaders.of(
                "Content-Type" to "multipart/form-data; boundary=$boundary"
            )
            val request = HttpRequest(
                method = HttpMethod.POST,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = headers,
                bodyStream = ByteArrayInputStream(bodyContent.toByteArray(Charset.defaultCharset()))
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

        /**
         * Test for missing boundary in multipart body.
         */
        @Test
        fun `test bodyAsMultipart throws exception if boundary is missing`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "multipart/form-data"
            )
            val request = HttpRequest(
                method = HttpMethod.POST,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = headers,
                bodyStream = ByteArrayInputStream(ByteArray(0))
            )
            val context = Context(request, emptyMap())

            val exception = assertFailsWith<IllegalStateException> { context.bodyAsMultipart() }
            assertEquals("Missing boundary parameter", exception.message)
        }

        /**
         * Test for invalid Content-Type for multipart body.
         */
        @Test
        fun `test bodyAsMultipart throws exception for invalid Content-Type`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json"
            )
            val request = HttpRequest(
                method = HttpMethod.POST,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = headers,
                bodyStream = ByteArrayInputStream(ByteArray(0))
            )
            val context = Context(request, emptyMap())

            val exception = assertFailsWith<IllegalStateException> { context.bodyAsMultipart() }
            assertEquals("Content-Type must be multipart/form-data", exception.message)
        }

        /**
         * Test for calling bodyAsMultipart after body stream is already read.
         */
        @Test
        fun `test bodyAsMultipart throws exception if body stream already read`() {
            val boundary = "----BoundaryXYZ"
            val bodyContent = """
            ------BoundaryXYZ
            Content-Disposition: form-data; name="field1"

            value1
            ------BoundaryXYZ--
        """.trimIndent().replace("\n", "\r\n")
            val headers = HttpHeaders.of(
                "Content-Type" to "multipart/form-data; boundary=$boundary"
            )
            val request = HttpRequest(
                method = HttpMethod.POST,
                path = RequestTarget("/test"),
                protocol = HttpProtocol.HTTP_1_1,
                headers = headers,
                bodyStream = ByteArrayInputStream(bodyContent.toByteArray(Charset.defaultCharset()))
            )
            val context = Context(request, emptyMap())

            // Read the body as text first to consume the stream
            context.bodyAsText()

            val exception = assertFailsWith<IllegalStateException> { context.bodyAsMultipart() }
            assertEquals("Body stream has already been read for multipart parsing", exception.message)
        }
    }
}