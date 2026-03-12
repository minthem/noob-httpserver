package io.github.minthem.noobhttpserver.http


import org.junit.jupiter.api.Nested
import kotlin.test.*

class HttpHeadersTest {

    @Nested
    inner class GetTest {
        @Test
        fun `returns null when headers are empty`() {
            val headers = ImmutableHttpHeaders(emptyMap())
            val result = headers["Content-Type"]
            assertNull(result)
        }

        @Test
        fun `returns the first value`() {
            val headers = ImmutableHttpHeaders(mapOf("Content-Type" to listOf("application/json", "text/html")))
            val result = headers["Content-Type"]
            assertEquals(
                "application/json",
                result
            )
        }

        @Test
        fun `returns the first value as is`() {
            val headers = ImmutableHttpHeaders(mapOf("Content-Type" to listOf("text/html, text/plan", "text/xml")))
            val result = headers["Content-Type"]
            assertEquals(
                "text/html, text/plan",
                result
            )
        }

        @Test
        fun `returns the value for the specified key`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            val result = headers["Accept"]
            assertEquals(
                "text/plain",
                result
            )
        }

        @Test
        fun `returns null when the specified key does not exist`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            val result = headers["X-Custom-Header"]
            assertNull(result)
        }

        @Test
        fun `ignores key case`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            val result = headers["aCcEpT"]
            assertEquals("text/plain", result)
        }
    }

    @Nested
    inner class GetAllTest {
        @Test
        fun `returns an empty list when headers are empty`() {
            val headers = ImmutableHttpHeaders(emptyMap())
            val result = headers.getAll("Content-Type")
            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns all values for the specified key`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            val result = headers.getAll("Accept")
            assertEquals(
                listOf("text/plain", "text/html"),
                result
            )
        }

        @Test
        fun `returns an empty list when the specified key does not exist`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            val result = headers.getAll("X-Custom-Header")
            assertTrue(result.isEmpty())
        }

        @Test
        fun `ignores key case in getAll`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            val result = headers.getAll("aCcEpT")
            assertEquals(listOf("text/plain", "text/html"), result)
        }
    }

    @Nested
    inner class GetJoinedTest {
        @Test
        fun `returns null when headers are empty in getJoined`() {
            val headers = ImmutableHttpHeaders(emptyMap())
            val result = headers.getJoined("Content-Type")
            assertNull(result)
        }

        @Test
        fun `returns joined values for the specified key`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html,text/xml")
                )
            )
            val result = headers.getJoined("Accept")
            assertEquals(
                "text/plain, text/html,text/xml",
                result
            )
        }

        @Test
        fun `returns null when the specified key does not exist in getJoined`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            val result = headers.getJoined("X-Custom-Header")
            assertNull(result)
        }

        @Test
        fun `ignores key case in getJoined`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            val result = headers.getJoined("aCcEpT")
            assertEquals("text/plain, text/html", result)
        }
    }

    @Nested
    inner class ContainsTest {
        @Test
        fun `returns true when the header exists`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            assertTrue(headers.contains("Content-Type"))
        }

        @Test
        fun `returns false when the header does not exist`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            assertFalse(headers.contains("Authorization"))
        }

        @Test
        fun `ignores key case in contains`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            assertTrue(headers.contains("aCcEpT"))
        }
    }

    @Nested
    inner class EqualsTest {
        @Test
        fun `returns false when compared with null`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            assertFalse(headers.equals(null))
        }

        @Test
        fun `returns false when compared with a different type`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json", "text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            assertFalse(headers.equals("not a HttpHeaders instance"))
        }

        @Test
        fun `returns true when headers have the same values`() {
            val headers1 = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            val headers2 = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            assertTrue(headers1 == headers2)
        }

        @Test
        fun `returns false when headers do not have the same values`() {
            val headers1 = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            val headers2 = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("text/html"),
                    "Accept" to listOf("text/plain", "text/html")
                )
            )
            assertFalse(headers1 == headers2)
        }

        @Test
        fun `compares header names case-insensitively`() {
            val headers1 = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json"),
                    "ACCEPT" to listOf("text/plain", "text/html")
                )
            )
            val headers2 = ImmutableHttpHeaders(
                mapOf(
                    "content-type" to listOf("application/json"),
                    "accept" to listOf("text/plain", "text/html")
                )
            )
            assertTrue(headers1 == headers2)
        }


        @Test
        fun `compares header values case-sensitively`() {
            val headers1 = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("APPLICATION/JSON"),
                    "ACCEPT" to listOf("text/plain", "text/html")
                )
            )
            val headers2 = ImmutableHttpHeaders(
                mapOf(
                    "content-type" to listOf("application/json"),
                    "accept" to listOf("text/plain", "text/html")
                )
            )
            assertFalse(headers1 == headers2)
        }
    }

    @Nested
    inner class AddTest {
        @Test
        fun `adds a value to a non-existent header`() {
            val headers = MutableHttpHeaders(emptyMap())
            headers.add("Content-Type", "application/json")
            assertEquals(
                listOf("application/json"),
                headers.getAll("Content-Type")
            )
        }

        @Test
        fun `appends a value to an existing header`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json",
            ).toMutable()
            headers.add("Content-Type", "text/html")

            assertEquals(
                listOf("application/json", "text/html"),
                headers.getAll("Content-Type")
            )
        }

        @Test
        fun `ignores key case when adding a value`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json",
            ).toMutable()
            headers.add("content-type", "text/html")

            assertEquals(
                listOf("application/json", "text/html"),
                headers.getAll("Content-Type")
            )
        }

        @Test
        fun `adds multiple values at once`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json",
            ).toMutable()
            headers.add(
                "Content-Type" to "text/html",
                "Accept" to "application/xml",
                "Accept" to "text/plain"
            )

            val expected = HttpHeaders.of(
                "Content-Type" to "application/json",
                "Content-Type" to "text/html",
                "Accept" to "application/xml",
                "Accept" to "text/plain"
            )

            assertEquals(
                expected,
                headers
            )
        }
    }

    @Nested
    inner class AddAllTest {
        @Test
        fun `adds all values to a non-existent header`() {
            val headers = MutableHttpHeaders(emptyMap())
            headers.addAll("Content-Type", listOf("application/json", "text/html"))
            assertEquals(
                listOf("application/json", "text/html"),
                headers.getAll("Content-Type")
            )
        }

        @Test
        fun `appends all values to an existing header`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json",
            ).toMutable()
            headers.addAll("Content-Type", listOf("text/html", "application/xml"))

            assertEquals(
                listOf("application/json", "text/html", "application/xml"),
                headers.getAll("Content-Type")
            )
        }

        @Test
        fun `ignores key case when adding all values`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json",
            ).toMutable()
            headers.addAll("content-type", listOf("text/html", "application/xml"))

            assertEquals(
                listOf("application/json", "text/html", "application/xml"),
                headers.getAll("Content-Type")
            )
        }
    }

    @Nested
    inner class SetTest {
        @Test
        fun `sets a value for a non-existent header`() {
            val headers = MutableHttpHeaders(emptyMap())
            headers["Content-Type"] = "application/json"
            assertEquals(
                listOf("application/json"),
                headers.getAll("Content-Type")
            )
        }

        @Test
        fun `overwrites the value of an existing header`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json",
            ).toMutable()
            headers["Content-Type"] = "text/html"

            assertEquals(
                listOf("text/html"),
                headers.getAll("Content-Type")
            )
        }

        @Test
        fun `ignores key case when setting a value`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json",
            ).toMutable()
            headers["content-type"] = "text/html"

            assertEquals(
                listOf("text/html"),
                headers.getAll("Content-Type")
            )
        }

        @Test
        fun `removes the header when setting null`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json",
            ).toMutable()
            headers["Content-Type"] = null

            assertFalse(headers.contains("Content-Type"))
        }
    }

    @Nested
    inner class RemoveTest {
        @Test
        fun `removes the header`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json",
            ).toMutable()
            headers.remove("Content-Type")
            assertFalse(headers.contains("Content-Type"))
        }

        @Test
        fun `does nothing when removing a non-existent header`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json",
            ).toMutable()
            headers.remove("Authorization")
            assertTrue(headers.contains("Content-Type"))
        }
    }

    @Nested
    inner class FactoryTest {
        @Test
        fun `of map should normalize keys and merge values with different cases`() {
            val headers = HttpHeaders.of(
                mapOf(
                    "Content-Type" to listOf("application/json"),
                    "content-type" to listOf("text/html"),
                    "ACCEPT" to listOf("text/plain")
                )
            )

            assertEquals(listOf("application/json", "text/html"), headers.getAll("Content-Type"))
            assertEquals(listOf("text/plain"), headers.getAll("accept"))
        }

        @Test
        fun `of vararg should preserve insertion order of values for same key`() {
            val headers = HttpHeaders.of(
                "Accept" to "application/json",
                "accept" to "text/html",
                "ACCEPT" to "text/plain"
            )

            assertEquals(
                listOf("application/json", "text/html", "text/plain"),
                headers.getAll("Accept")
            )
        }
    }

    @Nested
    inner class ConversionTest {
        @Test
        fun `toImmutable should create independent snapshot from mutable headers`() {
            val mutable = MutableHttpHeaders()
            mutable.add("Content-Type", "application/json")

            val immutable = mutable.toImmutable()
            mutable.add("Content-Type", "text/html")
            mutable["Accept"] = "text/plain"

            assertEquals(listOf("application/json"), immutable.getAll("Content-Type"))
            assertTrue(immutable.getAll("Accept").isEmpty())
        }

        @Test
        fun `toMutable should create independent mutable copy from immutable headers`() {
            val immutable = HttpHeaders.of(
                "Content-Type" to "application/json",
                "Accept" to "text/plain"
            ).toImmutable()

            val mutable = immutable.toMutable()
            mutable.add("Content-Type", "text/html")
            mutable["Accept"] = "application/xml"
            mutable["Authorization"] = "Bearer token"

            assertEquals(listOf("application/json"), immutable.getAll("Content-Type"))
            assertEquals(listOf("text/plain"), immutable.getAll("Accept"))
            assertFalse("Authorization" in immutable)
        }

        @Test
        fun `toMutable should return same instance for mutable headers`() {
            val headers = MutableHttpHeaders()
            val result = headers.toMutable()

            assertSame(headers, result)
        }

        @Test
        fun `toImmutable should return same instance for immutable headers`() {
            val headers = HttpHeaders.of("Content-Type" to "application/json").toImmutable()
            val result = headers.toImmutable()

            assertSame(headers, result)
        }
    }

    @Nested
    inner class ForEachTest {
        @Test
        fun `forEach should iterate over normalized keys and copied values`() {
            val headers = HttpHeaders.of(
                "Content-Type" to "application/json",
                "content-type" to "text/html",
                "Accept" to "text/plain"
            )

            val actual = mutableMapOf<String, List<String>>()
            headers.forEach { key, values ->
                actual[key] = values
            }

            assertEquals(
                mapOf(
                    "content-type" to listOf("application/json", "text/html"),
                    "accept" to listOf("text/plain")
                ),
                actual
            )
        }

        @Test
        fun `forEach should provide defensive copy of values`() {
            val headers = HttpHeaders.of(
                "Accept" to "application/json",
                "Accept" to "text/html"
            )

            headers.forEach { _, values ->
                if (values is MutableList<String>) {
                    values.add("application/xml")
                }
            }

            assertEquals(
                listOf("application/json", "text/html"),
                headers.getAll("Accept")
            )
        }
    }

    @Nested
    inner class HashCodeTest {
        @Test
        fun `returns same hashCode when headers are equal`() {
            val headers1 = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json"),
                    "ACCEPT" to listOf("text/plain", "text/html")
                )
            )
            val headers2 = ImmutableHttpHeaders(
                mapOf(
                    "content-type" to listOf("application/json"),
                    "accept" to listOf("text/plain", "text/html")
                )
            )

            assertEquals(headers1, headers2)
            assertEquals(headers1.hashCode(), headers2.hashCode())
        }
    }
}
