package io.github.minthem.noobhttpserver.http

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import java.io.ByteArrayInputStream

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
}