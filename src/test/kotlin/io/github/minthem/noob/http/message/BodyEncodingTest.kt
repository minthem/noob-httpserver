package io.github.minthem.noob.http.message

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyEncodingTest {
    @Nested
    inner class ParseTest {
        @Test
        fun `should parse body encoding`() {
            val actual = BodyEncoding.parse("identity")

            assertEquals("identity", actual.type)
            assertNull(actual.quality)
        }

        @Test
        fun `should parse body encoding with quality`() {
            val actual = BodyEncoding.parse("gzip;q=0.5")

            assertEquals("gzip", actual.type)
            assertEquals(0.5, actual.quality)
        }

        @Test
        fun `should parse body encoding multiple times`() {
            val actual = BodyEncoding.parse("gzip;q=0.5, identity;q=0.8")

            assertEquals("gzip", actual.type)
            assertEquals(0.5, actual.quality)
        }

        @Test
        fun `should throw exception when type is empty`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    BodyEncoding.parse("")
                }
            assertEquals("Invalid body encoding (type is empty)", exception.message)
        }

        @Test
        fun `should throw exception when type contains invalid characters`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    BodyEncoding.parse("invalid @ type")
                }
            assertEquals("Invalid body encoding (type contains invalid characters: invalid @ type)", exception.message)
        }

        @Test
        fun `should throw exception when quality is out of range`() {
            val exception1 =
                assertThrows<IllegalArgumentException> {
                    BodyEncoding.parse("gzip;q=1.1")
                }
            assertEquals("Invalid body encoding quality value (must be between 0.0 and 1.0): 1.1", exception1.message)

            val exception2 =
                assertThrows<IllegalArgumentException> {
                    BodyEncoding.parse("gzip;q=-0.1")
                }
            assertEquals("Invalid body encoding quality value (must be between 0.0 and 1.0): -0.1", exception2.message)
        }

        @Test
        fun `should throw exception when quality is not a valid number`() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    BodyEncoding.parse("gzip;q=abc")
                }
            assertEquals("Invalid body encoding quality value: abc", exception.message)
        }
    }

    @Nested
    inner class ParseAllTest {
        @Test
        fun `should parse all body encodings`() {
            val actual = BodyEncoding.parseAll("gzip;q=0.5, identity;q=0.8, *;q=0.1")
            assertEquals(3, actual.size)
            assertEquals("gzip", actual[0].type)
            assertEquals(0.5, actual[0].quality)
            assertEquals("identity", actual[1].type)
            assertEquals(0.8, actual[1].quality)
            assertEquals("*", actual[2].type)
            assertEquals(0.1, actual[2].quality)
        }

        @Test
        fun `should ignore invalid body encodings and parse valid ones`() {
            // "invalid @ type" と "deflate;q=abc" は不正なため無視され、gzip と identity だけが残る
            val actual = BodyEncoding.parseAll("gzip;q=0.5, invalid @ type, identity;q=0.8, deflate;q=abc")

            assertEquals(2, actual.size)
            assertEquals("gzip", actual[0].type)
            assertEquals(0.5, actual[0].quality)
            assertEquals("identity", actual[1].type)
            assertEquals(0.8, actual[1].quality)
        }
    }

    @Nested
    inner class ToStringTest {
        @Test
        fun `should return string representation of body encoding`() {
            val encoding = BodyEncoding.parse("gzip")
            assertEquals("gzip", encoding.toString())
        }

        @Test
        fun `should return string representation of body encoding with quality`() {
            val encoding = BodyEncoding.parse("gzip;q=0.5")
            assertEquals("gzip;q=0.5", encoding.toString())
        }
    }

    @Nested
    inner class CompareTest {
        @Test
        fun `should compare body encodings`() {
            val encoding1 = BodyEncoding.parse("gzip;q=0.5")
            val encoding2 = BodyEncoding.parse("gzip;q=0.8")
            assertTrue(encoding1 < encoding2)
            assertTrue(encoding2 > encoding1)
            assertNotEquals(encoding1, encoding2)
        }

        @Test
        fun `should compare body encodings with different types`() {
            val encoding1 = BodyEncoding.parse("gzip;q=0.5")
            val encoding2 = BodyEncoding.parse("deflate;q=0.8")
            assertTrue(encoding1 < encoding2)
            assertTrue(encoding2 > encoding1)
            assertNotEquals(encoding1, encoding2)
        }
    }
}
