package io.github.minthem.noobhttpserver.http

import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the HttpResponse class.
 *
 * The HttpResponse class represents an HTTP response with methods for setting the response body
 * from a text, byte array, or file. This test suite validates the functionality of its API
 * including the proper handling of headers, status, and body content.
 */
class HttpResponseTest {

    @Test
    fun `should create HttpResponse with default values`() {
        // Act
        val response = HttpResponse()

        // Assert
        assertEquals(HttpStatus.OK, response.status)
        assertTrue(response.headers is MutableHttpHeaders)
        assertTrue(response.body is EmptyResponseBody)
    }

    @Test
    fun `should allow setting body from text`() {
        // Arrange
        val response = HttpResponse()
        val content = "Hello, World!"
        val charset = StandardCharsets.UTF_8

        // Act
        response.bodyFromText(content, charset)

        // Assert
        assertTrue(response.body is TextResponseBody)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val actual = String(bStream.toByteArray(), charset)
        assertEquals(content, actual)
        assertEquals("text/plain; charset=UTF-8", response.body.defaultContentType())
    }

    @Test
    fun `should allow setting body from bytes`() {
        // Arrange
        val response = HttpResponse()
        val content = "Hello, World!".toByteArray(StandardCharsets.UTF_8)

        // Act
        response.bodyFromBytes(content)

        // Assert
        assertTrue(response.body is BinaryResponseBody)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val actual = bStream.toByteArray()
        assertContentEquals(content, actual)

        assertEquals("application/octet-stream", response.body.defaultContentType())
    }

    @Test
    fun `should allow setting body from file`() {
        // Arrange
        val response = HttpResponse()
        val content = "File Content"
        val tempFile = Files.createTempFile("test", ".txt").apply {
            toFile().writeText(content)
        }

        // Act
        response.bodyFromFile(tempFile)

        // Assert
        assertTrue(response.body is FileResponseBody)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val actual = String(bStream.toByteArray())
        assertEquals(content, actual)
        assertEquals("text/plain; charset=UTF-8", response.body.defaultContentType())

        // Cleanup
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `should allow creating an HttpResponse with ok status`() {
        // Act
        val response = HttpResponse.ok()

        // Assert
        assertEquals(HttpStatus.OK, response.status)
        assertTrue(response.headers is MutableHttpHeaders)
        assertNotNull(response.headers)
    }
}