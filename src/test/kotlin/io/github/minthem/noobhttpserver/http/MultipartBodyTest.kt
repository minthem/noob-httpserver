package io.github.minthem.noobhttpserver.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.test.assertContentEquals

internal class MultipartBodyTest {

    /**
     * Tests the `part(name: String)` method which retrieves a specific multipart body part
     * by its name. If the part with the specified name is already read, it retrieves it from
     * the cached parts; otherwise, it parses the input stream until the part is found or returns null.
     */
    @Test
    fun `test retrieving single part by name`() {
        val input = """
            --boundary
            Content-Disposition: form-data; name="part1"

            value1
            --boundary
            Content-Disposition: form-data; name="part2"

            value2
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part = body.part("part1")

        assertNotNull(part)
        assertEquals("value1", (part as Multipart.FormField).value)
        assertEquals("part1", part.name)
    }

    /**
     * Tests the behavior of `part(name: String)` when the requested multipart body part
     * does not exist in the stream.
     */
    @Test
    fun `test retrieving non-existing part by name`() {
        val input = """
            --boundary
            Content-Disposition: form-data; name="part1"

            value1
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part = body.part("nonexistent")

        assertNull(part)
    }

    /**
     * Tests the caching mechanism of `part(name: String)`. Once a part is read and cached,
     * subsequent calls to `part(name: String)` should retrieve it from the cache without
     * reprocessing the stream.
     */
    @Test
    fun `test cached part retrieval`() {
        val input = """
            --boundary
            Content-Disposition: form-data; name="part1"

            value1
            --boundary
            Content-Disposition: form-data; name="part2"

            value2
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part1 = body.part("part1")
        val part2 = body.part("part1") // Retrieve the same part again

        assertNotNull(part1)
        assertNotNull(part2)
        assertEquals(part1, part2) // Verify cached object equality
    }

    /**
     * Tests the `forEachPart(block: (Multipart) -> Unit)` method which processes each multipart body part
     * in the stream and passes it to the provided block for handling.
     */
    @Test
    fun `test processing all parts with forEachPart`() {
        val input = """
            --boundary
            Content-Disposition: form-data; name="part1"

            value1
            --boundary
            Content-Disposition: form-data; name="part2"

            value2
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val parts = mutableListOf<Multipart>()

        body.forEachPart { part -> parts.add(part) }

        assertEquals(2, parts.size)
        assertEquals("value1", (parts[0] as Multipart.FormField).value)
        assertEquals("part1", parts[0].name)
        assertEquals("value2", (parts[1] as Multipart.FormField).value)
        assertEquals("part2", parts[1].name)
    }

    /**
     * Tests the `forEachPart(block: (Multipart) -> Unit)` method when no parts exist in the input stream.
     * Ensures the method gracefully completes without invoking the block.
     */
    @Test
    fun `test forEachPart when stream has no parts`() {
        val input = """
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val parts = mutableListOf<Multipart>()

        body.forEachPart { part -> parts.add(part) }

        assertEquals(0, parts.size)
    }

    /**
     * Tests invalid multipart input stream handling for the `part(name: String)` method.
     * Checks for an exception when the input stream lacks a `Content-Disposition` header.
     */
    @Test
    fun `test invalid multipart input without content-disposition`() {
        val input = """
            --boundary
            Content-Type: text/plain

            invalid part
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        assertFailsWith<IllegalArgumentException> {
            body.part("part1")
        }
    }

    @Test
    fun `test forEachPart after part should include cached and unread parts without duplication`() {
        val input = """
            --boundary
            Content-Disposition: form-data; name="part1"

            value1
            --boundary
            Content-Disposition: form-data; name="part2"

            value2
            --boundary
            Content-Disposition: form-data; name="part3"

            value3
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part2 = body.part("part2")
        assertNotNull(part2)
        assertEquals("value2", (part2 as Multipart.FormField).value)

        val parts = mutableListOf<Multipart>()
        body.forEachPart { part -> parts.add(part) }

        assertEquals(3, parts.size)
        assertEquals(listOf("part1", "part2", "part3"), parts.map { it.name })
        assertEquals(listOf("value1", "value2", "value3"), parts.map { (it as Multipart.FormField).value })
    }

    @Test
    fun `test part after forEachPart should return cached part`() {
        val input = """
            --boundary
            Content-Disposition: form-data; name="part1"

            value1
            --boundary
            Content-Disposition: form-data; name="part2"

            value2
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val parts = mutableListOf<Multipart>()
        body.forEachPart { part -> parts.add(part) }

        val part = body.part("part2")

        assertNotNull(part)
        assertEquals("part2", part.name)
        assertEquals("value2", (part as Multipart.FormField).value)
        assertEquals(parts[1], part)
    }

    @Test
    fun `test retrieving file upload part by name`() {
        val input = """
            --boundary
            Content-Disposition: form-data; name="file"; filename="hello.txt"
            Content-Type: text/plain

            hello file
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part = body.part("file")

        assertNotNull(part)
        assertTrue(part is Multipart.FileUpload)
        assertEquals("file", part.name)
        assertEquals("hello.txt", part.filename)
        assertEquals("hello file", String(part.asStream().readBytes()))
    }

    @Test
    fun `test duplicate part names should keep latest part in cache`() {
        val input = """
            --boundary
            Content-Disposition: form-data; name="part1"

            first
            --boundary
            Content-Disposition: form-data; name="part1"

            second
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val parts = mutableListOf<Multipart>()
        body.forEachPart { part -> parts.add(part) }

        val cached = body.part("part1")

        assertEquals(2, parts.size)
        assertEquals("first", (parts[0] as Multipart.FormField).value)
        assertEquals("second", (parts[1] as Multipart.FormField).value)
        assertNotNull(cached)
        assertEquals("second", (cached as Multipart.FormField).value)
    }

    @Test
    fun `test close should clear cached parts and delete temp files`() {
        val largeContent = "a".repeat(1024 * 1024 + 1)
        val input = """
            --boundary
            Content-Disposition: form-data; name="file"; filename="large.txt"
            Content-Type: text/plain

            $largeContent
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part = body.part("file")
        assertNotNull(part)
        assertTrue(part is Multipart.FileUpload)
        assertNotNull(part.savePath)
        assertTrue(Files.exists(part.savePath))
        val fileContent = Files.readString(part.savePath)
        assertEquals(largeContent, fileContent)
        val streamContent = String(part.asStream().readBytes())
        assertEquals(largeContent, streamContent)

        body.close()

        assertTrue(Files.notExists(part.savePath))
        assertNull(body.part("file"))
    }

    @Test
    fun `test close is safe when no file upload parts were read`() {
        val input = """
            --boundary
            Content-Disposition: form-data; name="part1"

            value1
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        body.part("part1")
        body.close()
        body.close() // idempotent-ish behavior should not throw
    }

    @Test
    fun `test file upload stream can be reused multiple times`() {
        val input = """
            --boundary
            Content-Disposition: form-data; name="file"; filename="hello.txt"
            Content-Type: text/plain

            hello file
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part = body.part("file")
        assertNotNull(part)
        assertTrue(part is Multipart.FileUpload)
        val st1 = part.asStream()
        val st2 = part.asStream()

        assertContentEquals(st1.readBytes(), st2.readBytes())

        body.close()
    }

    @Test
    fun `test large file upload stream can be reused multiple times`() {
        val largeContent = "a".repeat(1024 * 1024 + 1)
        val input = """
            --boundary
            Content-Disposition: form-data; name="file"; filename="large.txt"
            Content-Type: text/plain

            $largeContent
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part = body.part("file")
        assertNotNull(part)
        assertTrue(part is Multipart.FileUpload)
        val st1 = part.asStream()
        val st2 = part.asStream()

        assertContentEquals(st1.readBytes(), st2.readBytes())

        body.close()
    }

    @Test
    fun `copyTo copies file contents correctly`() {
        val input = """
            --boundary
            Content-Disposition: form-data; name="file"; filename="hello.txt"
            Content-Type: text/plain

            hello file
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part = body.part("file")
        assertNotNull(part)
        assertTrue(part is Multipart.FileUpload)
        val tempFile = Files.createTempFile("multipart-test", ".txt")

        part.copyTo(tempFile)

        val actual = Files.readString(tempFile)
        assertEquals("hello file", actual)
    }

    @Test
    fun `copyTo overwrites existing file contents`() {
        val content = "original content"
        val input = """
            --boundary
            Content-Disposition: form-data; name="file"; filename="hello.txt"
            Content-Type: text/plain
        
            $content
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part = body.part("file")
        assertNotNull(part)
        assertTrue(part is Multipart.FileUpload)

        val tempFile = Files.createTempFile("multipart-test", ".txt")
        Files.writeString(tempFile, "existing content, should be overwritten")

        part.copyTo(tempFile)

        val actual = Files.readString(tempFile)
        assertEquals(content, actual)
    }

    @Test
    fun `large form field across buffer boundary is parsed correctly`() {
        val largeContent = "a".repeat(4096 * 2 + 123)
        val input = """
            --boundary
            Content-Disposition: form-data; name="message"
            Content-Type: text/plain
        
            $largeContent
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part = body.part("message")

        assertNotNull(part)
        assertTrue(part is Multipart.FormField)
        assertEquals("message", part.name)
        assertEquals(largeContent, part.value)
    }

    @Test
    fun `large file upload across buffer boundary can be copied correctly`() {
        val largeContent = "b".repeat(4096 * 2 + 123)
        val input = """
            --boundary
            Content-Disposition: form-data; name="file"; filename="large.txt"
            Content-Type: text/plain
        
            $largeContent
            --boundary--
        """.trimIndent().replace("\n", "\r\n").toByteArray()

        val body = MultipartBody(
            stream = ByteArrayInputStream(input),
            boundary = "boundary"
        )

        val part = body.part("file")

        assertNotNull(part)
        assertTrue(part is Multipart.FileUpload)

        val tempFile = Files.createTempFile("multipart-test-large", ".txt")
        part.copyTo(tempFile)

        val actual = Files.readString(tempFile)
        assertEquals(largeContent, actual)
    }
}