package io.github.minthem.noobhttpserver.http

import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for the UriDecoder class, focusing on the decodePath function.
 * The decodePath function decodes URI-encoded strings into their original form
 * using UTF-8 encoding.
 */
internal class UriDecoderTest {

    @Nested
    inner class DecodePathTest {
        @Test
        fun `should decode simple percent-encoded path`() {
            val input = "hello%20world"
            val expected = "hello world"
            val result = UriDecoder.decodePath(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }

        @Test
        fun `should decode special characters in path`() {
            val input = "%40%23%24%25"
            val expected = "@#$%"
            val result = UriDecoder.decodePath(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }

        @Test
        fun `should return the same string if no percent-encoding exists`() {
            val input = "simplePath"
            val expected = "simplePath"
            val result = UriDecoder.decodePath(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }

        @Test
        fun `should decode utf8 percent encoded characters in path`() {
            val input = "%E3%81%82"
            val expected = "あ"
            val result = UriDecoder.decodePath(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }

        @Test
        fun `should handle empty string as input`() {
            val input = ""
            val expected = ""
            val result = UriDecoder.decodePath(input)
            assertEquals(expected, result, "Decoding failed for empty input")
        }

        @Test
        fun `should decode path with mixed percent-encoded and plain text`() {
            val input = "mixed%20content%2FplainText"
            val expected = "mixed content/plainText"
            val result = UriDecoder.decodePath(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }

        @Test
        fun `should not decode plus as space in path`() {
            val input = "hello+world"
            val expected = "hello+world"
            val result = UriDecoder.decodePath(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }
    }

    @Nested
    inner class DecodeQueryTest {
        @Test
        fun `should decode simple percent-encoded query`() {
            val input = "hello%20world"
            val expected = "hello world"
            val result = UriDecoder.decodeQuery(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }

        @Test
        fun `should decode special characters in query`() {
            val input = "%40%23%24%25"
            val expected = "@#$%"
            val result = UriDecoder.decodeQuery(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }

        @Test
        fun `should return the same string if no percent-encoding exists`() {
            val input = "simpleQuery"
            val expected = "simpleQuery"
            val result = UriDecoder.decodeQuery(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }

        @Test
        fun `should throw exception for incomplete percent encoding with missing digits`() {
            val input = "hello%"
            assertFailsWith<IllegalArgumentException>(
                message = "Expected exception for incomplete percent encoding: $input"
            ) {
                UriDecoder.decodeQuery(input)
            }
        }

        @Test
        fun `should throw exception for invalid percent encoding characters`() {
            val input = "hello%G1"
            assertFailsWith<IllegalArgumentException>(
                message = "Expected exception for invalid percent encoding: $input"
            ) {
                UriDecoder.decodeQuery(input)
            }
        }

        @Test
        fun `should handle empty string as input`() {
            val input = ""
            val expected = ""
            val result = UriDecoder.decodeQuery(input)
            assertEquals(expected, result, "Decoding failed for empty input")
        }

        @Test
        fun `should decode query with mixed percent-encoded and plain text`() {
            val input = "mixed%20content%2FplainText"
            val expected = "mixed content/plainText"
            val result = UriDecoder.decodeQuery(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }

        @Test
        fun `should decode utf8 percent encoded characters in query`() {
            val input = "%E3%81%82"
            val expected = "あ"
            val result = UriDecoder.decodeQuery(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }
        @Test
        fun `should decode utf8 percent encoded characters and ascii in query`() {
            val input = "%E3%81%82abc%E7%84%BC%E3%81%8D%E3%81%9D%E3%81%B0"
            val expected = "あabc焼きそば"
            val result = UriDecoder.decodeQuery(input)
            assertEquals(expected, result, "Decoding failed for input: $input")
        }
    }
}