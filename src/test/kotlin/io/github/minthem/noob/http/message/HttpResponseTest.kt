package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.util.asCloseable
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
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
        assertTrue(response.body is EmptyBodyProducer)
    }

    @Test
    fun `should allow setting body from text`() {
        // Arrange
        val content = "Hello, World!"
        val charset = StandardCharsets.UTF_8

        // Act
        val response =
            HttpResponse.build {
                body(content, charset)
            }

        // Assert
        assertTrue(response.body is TextBodyProducer)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val actual = String(bStream.toByteArray(), charset)
        assertEquals(content, actual)
        assertEquals("text/plain; charset=\"utf-8\"", response.body.defaultContentType.toString())
    }

    @Test
    fun `should allow setting body from bytes`() {
        // Arrange
        val content = "Hello, World!".toByteArray(StandardCharsets.UTF_8)

        // Act
        val response =
            HttpResponse.build {
                body(content)
            }

        // Assert
        assertTrue(response.body is BinaryBodyProducer)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val actual = bStream.toByteArray()
        assertContentEquals(content, actual)

        assertEquals("application/octet-stream", response.body.defaultContentType.toString())
    }

    @Test
    fun `should allow setting body from file`() {
        // Arrange
        val content = "File Content"
        val tempFile =
            Files.createTempFile("test", ".txt").apply {
                toFile().writeText(content)
            }

        // Act
        val response =
            HttpResponse.build {
                body(tempFile)
            }

        // Assert
        assertTrue(response.body is FileBodyProducer)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val actual = String(bStream.toByteArray())
        assertEquals(content, actual)
        assertEquals("text/plain; charset=\"utf-8\"", response.body.defaultContentType.toString())

        // Cleanup
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `should allow setting body from html file`() {
        // Arrange
        val content = "<html><body><h1>Hello World</h1></body></html>"
        val tempFile =
            Files.createTempFile("test", ".html").apply {
                toFile().writeText(content)
            }

        // Act
        val response =
            HttpResponse.build {
                body(tempFile)
            }

        // Assert
        assertTrue(response.body is FileBodyProducer)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val actual = String(bStream.toByteArray())
        assertEquals(content, actual)
        assertEquals("text/html; charset=\"utf-8\"", response.body.defaultContentType.toString())

        // Cleanup
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `should allow setting body from string chunk`() {
        // Arrange
        val bodySeq =
            sequenceOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 94)
                .map { "c".repeat(it) }
                .asCloseable { }

        // Act
        val response =
            HttpResponse.build {
                body(bodySeq)
            }

        // Assert
        assertTrue(response.body is StreamingBodyProducer)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val expected =
            listOf(
                "c",
                "cc",
                "ccc",
                "cccc",
                "ccccc",
                "cccccc",
                "ccccccc",
                "cccccccc",
                "ccccccccc",
                "cccccccccc",
                "ccccccccccc",
                "cccccccccccc",
                "ccccccccccccc",
                "cccccccccccccc",
                "ccccccccccccccc",
                "cccccccccccccccc",
                "ccccccccccccccccc",
                "c".repeat(94),
            ).joinToString("")
        val actual = String(bStream.toByteArray())
        assertEquals(expected, actual)

        assertEquals("application/octet-stream", response.body.defaultContentType.toString())
    }

    @Test
    fun `should allow setting body from byte array chunk`() {
        // Arrange
        val bodySeq =
            sequenceOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 94)
                .map { "c".repeat(it).toByteArray(StandardCharsets.UTF_8) }
                .asCloseable { }

        // Act
        val response =
            HttpResponse.build {
                body(bodySeq)
            }

        // Assert
        assertTrue(response.body is StreamingBodyProducer)
        val bStream = ByteArrayOutputStream()
        response.body.writeTo(Channels.newChannel(bStream))

        val expected =
            (
                listOf(
                    "c",
                    "cc",
                    "ccc",
                    "cccc",
                    "ccccc",
                    "cccccc",
                    "ccccccc",
                    "cccccccc",
                    "ccccccccc",
                    "cccccccccc",
                    "ccccccccccc",
                    "cccccccccccc",
                    "ccccccccccccc",
                    "cccccccccccccc",
                    "ccccccccccccccc",
                    "cccccccccccccccc",
                    "ccccccccccccccccc",
                    "c".repeat(94),
                ).joinToString("")
            ).toByteArray(StandardCharsets.UTF_8)

        val actual = bStream.toByteArray()
        assertContentEquals(expected, actual)

        assertEquals("application/octet-stream", response.body.defaultContentType.toString())
    }

    @Test
    fun `should apply headers when set`() {
        // Act
        val response =
            HttpResponse.build {
                header("Content-Type" to "text/html", "X-Custom-Header" to "Custom Value")
                header("X-Custom-Header" to "Second Value")
                header(
                    HttpHeaders.of(
                        "X-Custom-Header" to "Third Value",
                        "Vary" to "*",
                    ),
                )
                body("<h1>Hello, World!</h1>")
            }

        val expectedHeaders =
            HttpHeaders.of(
                mapOf(
                    "Content-Type" to listOf("text/html"),
                    "X-Custom-Header" to listOf("Custom Value", "Second Value", "Third Value"),
                    "Vary" to listOf("*"),
                ),
            )

        // Assert
        assertEquals(HttpStatus.OK, response.status)
        assertEquals(expectedHeaders, response.headers)
        assertTrue(response.body is TextBodyProducer)
    }

    @Test
    fun `should throw build response from file not found`() {
        // Arrange
        val nonExist = Path.of("non-existent-file.txt")

        // Act
        assertThrows<NoSuchFileException> {
            HttpResponse.build {
                body(nonExist)
            }
        }
    }

    @Test
    fun `should throw build response from file does not readable`() {
        // Arrange
        val content = "File Content"
        val tempFile =
            Files.createTempFile("test", ".txt").apply {
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

    @Nested
    inner class BuilderStatusTest {
        @Test
        fun `should apply status when set explicitly`() {
            val response =
                HttpResponse.build {
                    status = HttpStatus.CREATED
                }

            assertEquals(HttpStatus.CREATED, response.status)
        }
    }
}
