package io.github.minthem.noobhttpserver.http

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
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
        val response = HttpResponse.build { }

        // Assert
        assertEquals(HttpStatus.OK, response.status)
        assertTrue(response.headers is MutableHttpHeaders)
        assertTrue(response.body is EmptyBodyExecutor)
    }

    @Test
    fun `should allow setting body from text`() {
        // Arrange
        val content = "Hello, World!"
        val charset = StandardCharsets.UTF_8

        // Act
        val response = HttpResponse.build {
            body(content, charset)
        }

        // Assert
        assertTrue(response.body is TextBodyExecutor)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val actual = String(bStream.toByteArray(), charset)
        assertEquals(content, actual)
        assertEquals("text/plain; charset=\"UTF-8\"", response.body.defaultContentType().toString())
    }

    @Test
    fun `should allow setting body from bytes`() {
        // Arrange
        val content = "Hello, World!".toByteArray(StandardCharsets.UTF_8)

        // Act
        val response = HttpResponse.build {
            body(content)
        }

        // Assert
        assertTrue(response.body is BinaryBodyExecutor)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val actual = bStream.toByteArray()
        assertContentEquals(content, actual)

        assertEquals("application/octet-stream", response.body.defaultContentType().toString())
    }

    @Test
    fun `should allow setting body from file`() {
        // Arrange
        val content = "File Content"
        val tempFile = Files.createTempFile("test", ".txt").apply {
            toFile().writeText(content)
        }

        // Act
        val response = HttpResponse.build {
            body(tempFile)
        }

        // Assert
        assertTrue(response.body is FileBodyExecutor)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val actual = String(bStream.toByteArray())
        assertEquals(content, actual)
        assertEquals("text/plain; charset=\"UTF-8\"", response.body.defaultContentType().toString())

        // Cleanup
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `should allow setting body from html file`() {
        // Arrange
        val content = "<html><body><h1>Hello World</h1></body></html>"
        val tempFile = Files.createTempFile("test", ".html").apply {
            toFile().writeText(content)
        }

        // Act
        val response = HttpResponse.build {
            body(tempFile)
        }

        // Assert
        assertTrue(response.body is FileBodyExecutor)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val actual = String(bStream.toByteArray())
        assertEquals(content, actual)
        assertEquals("text/html; charset=\"UTF-8\"", response.body.defaultContentType().toString())

        // Cleanup
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `should allow setting body from string chunk`() {
        // Arrange
        val bodySeq = sequenceOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 94)
            .map { "c".repeat(it) }
            .asCloseable { }

        // Act
        val response = HttpResponse.build {
            body(bodySeq)
        }

        // Assert
        assertTrue(response.body is ChunkedBodyExecutor)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val expected = listOf(
            "1", "c",
            "2", "cc",
            "3", "ccc",
            "4", "cccc",
            "5", "ccccc",
            "6", "cccccc",
            "7", "ccccccc",
            "8", "cccccccc",
            "9", "ccccccccc",
            "a", "cccccccccc",
            "b", "ccccccccccc",
            "c", "cccccccccccc",
            "d", "ccccccccccccc",
            "e", "cccccccccccccc",
            "f", "ccccccccccccccc",
            "10", "cccccccccccccccc",
            "11", "ccccccccccccccccc",
            "5e", "c".repeat(94),
            "0", ""
        ).joinToString("\r\n") + "\r\n"
        val actual = String(bStream.toByteArray())
        assertEquals(expected, actual)

        assertEquals("application/octet-stream", response.body.defaultContentType().toString())
    }

    @Test
    fun `should allow setting body from byte array chunk`() {
        // Arrange
        val bodySeq = sequenceOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 94)
            .map { "c".repeat(it).toByteArray(StandardCharsets.UTF_8) }
            .asCloseable { }

        // Act
        val response = HttpResponse.build {
            body(bodySeq)
        }

        // Assert
        assertTrue(response.body is ChunkedBodyExecutor)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val expected = (listOf(
            "1", "c",
            "2", "cc",
            "3", "ccc",
            "4", "cccc",
            "5", "ccccc",
            "6", "cccccc",
            "7", "ccccccc",
            "8", "cccccccc",
            "9", "ccccccccc",
            "a", "cccccccccc",
            "b", "ccccccccccc",
            "c", "cccccccccccc",
            "d", "ccccccccccccc",
            "e", "cccccccccccccc",
            "f", "ccccccccccccccc",
            "10", "cccccccccccccccc",
            "11", "ccccccccccccccccc",
            "5e", "c".repeat(94),
            "0", ""
        ).joinToString("\r\n") + "\r\n").toByteArray(StandardCharsets.UTF_8)

        val actual = bStream.toByteArray()
        assertContentEquals(expected, actual)

        assertEquals("application/octet-stream", response.body.defaultContentType().toString())
    }

    @Test
    fun `should apply headers when set`() {
        // Act
        val response = HttpResponse.build {
            header("Content-Type" to "text/html", "X-Custom-Header" to "Custom Value")
            header("X-Custom-Header" to "Second Value")
            header(
                HttpHeaders.of(
                    "X-Custom-Header" to "Third Value",
                    "Vary" to "*"
                )
            )
            body("<h1>Hello, World!</h1>")
        }

        val expectedHeaders = HttpHeaders.of(
            mapOf(
                "Content-Type" to listOf("text/html"),
                "X-Custom-Header" to listOf("Custom Value", "Second Value", "Third Value"),
                "Vary" to listOf("*"),
                "Content-Length" to listOf("22")
            ),
        )

        // Assert
        assertEquals(HttpStatus.OK, response.status)
        assertEquals(expectedHeaders, response.headers)
        assertTrue(response.body is TextBodyExecutor)

    }

    @Test
    fun `should throw build response from file not found`() {
        // Arrange
        val nonExist = Path.of("non-existent-file.txt")

        // Act
        assertThrows<FileNotFoundException> {
            HttpResponse.build {
                body(nonExist)
            }
        }
    }

    @Test
    fun `should throw build response from file does not readable`() {
        // Arrange
        val content = "File Content"
        val tempFile = Files.createTempFile("test", ".txt").apply {
            toFile().writeText(content)
            toFile().setReadable(false)
        }

        // Act
        assertThrows<IOException> {
            HttpResponse.build {
                body(tempFile)
            }
        }

        // Cleanup
        Files.deleteIfExists(tempFile)
    }
}