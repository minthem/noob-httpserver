package io.github.minthem.noobhttpserver.http

import java.nio.charset.Charset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MediaTypeTest {

    @Test
    fun `parse should correctly parse type and subtype`() {
        val mediaType = MediaType.parse("text/plain")
        assertEquals("text", mediaType.type)
        assertEquals("plain", mediaType.subtype)
        assertTrue(mediaType.parameters.isEmpty())
    }

    @Test
    fun `parse should normalize type and subtype to lowercase`() {
        val mediaType = MediaType.parse("Text/Plain")
        assertEquals("text", mediaType.type)
        assertEquals("plain", mediaType.subtype)
    }

    @Test
    fun `parse should correctly parse parameters`() {
        val mediaType = MediaType.parse("text/plain; charset=UTF-8; q=0.9")
        assertEquals("text", mediaType.type)
        assertEquals("plain", mediaType.subtype)
        assertEquals(2, mediaType.parameters.size)
        assertEquals("UTF-8", mediaType.parameters["charset"])
        assertEquals("0.9", mediaType.parameters["q"])
    }

    @Test
    fun `parse should handle parameter without value as empty string`() {
        val mediaType = MediaType.parse("text/plain; charset")
        assertEquals("", mediaType.parameters["charset"])
    }

    @Test
    fun `parse should handle missing type or subtype`() {
        val wildcardMediaType = MediaType.parse("*/json")
        assertEquals("*", wildcardMediaType.type)
        assertEquals("json", wildcardMediaType.subtype)

        val wildcardBoth = MediaType.parse("*/*")
        assertEquals("*", wildcardBoth.type)
        assertEquals("*", wildcardBoth.subtype)
    }

    @Test
    fun `charset should return null when not defined`() {
        val mediaType = MediaType.parse("text/plain")
        assertNull(mediaType.charset)
    }

    @Test
    fun `charset should return charset object when defined`() {
        val mediaType = MediaType.parse("text/plain; charset=UTF-8")
        assertEquals(Charset.forName("UTF-8"), mediaType.charset)
    }

    @Test
    fun `isCompatibleWith should return true if types and subtypes match`() {
        val mediaType1 = MediaType.parse("text/plain")
        val mediaType2 = MediaType.parse("text/plain")
        assertTrue(mediaType1.isCompatibleWith(mediaType2))
    }

    @Test
    fun `isCompatibleWith should return false if types do not match`() {
        val mediaType1 = MediaType.parse("text/plain")
        val mediaType2 = MediaType.parse("application/json")
        assertFalse(mediaType1.isCompatibleWith(mediaType2))
    }

    @Test
    fun `isCompatibleWith should return true if left side contains wildcard`() {
        val mediaType1 = MediaType.parse("text/*")
        val mediaType2 = MediaType.parse("text/plain")

        assertTrue(mediaType1.isCompatibleWith(mediaType2))
    }

    @Test
    fun `isCompatibleWith should return true if right side contains wildcard`() {
        val mediaType1 = MediaType.parse("text/plain")
        val mediaType2 = MediaType.parse("*/*")

        assertTrue(mediaType1.isCompatibleWith(mediaType2))
    }

    @Test
    fun `isCompatibleWith should be symmetric for wildcard matching`() {
        val mediaType1 = MediaType.parse("text/*")
        val mediaType2 = MediaType.parse("text/plain")

        assertEquals(
            mediaType1.isCompatibleWith(mediaType2),
            mediaType2.isCompatibleWith(mediaType1)
        )
    }

    @Test
    fun `toString should return correct string representation without parameters`() {
        val mediaType = MediaType.parse("text/plain")
        assertEquals("text/plain", mediaType.toString())
    }

    @Test
    fun `toString should return correct string representation with parameters`() {
        val mediaType = MediaType.parse("text/plain; charset=UTF-8; q=0.9")
        assertEquals("text/plain; charset=\"UTF-8\"; q=\"0.9\"", mediaType.toString())
    }

    @Test
    fun `predefined multipart form data should match expected value`() {
        assertEquals("multipart", MediaType.MULTIPART_FORM_DATA.type)
        assertEquals("form-data", MediaType.MULTIPART_FORM_DATA.subtype)
    }

    @Test
    fun `predefined octet stream should match expected value`() {
        assertEquals("application", MediaType.OCTET_STREAM.type)
        assertEquals("octet-stream", MediaType.OCTET_STREAM.subtype)
    }
}