package io.github.minthem.noobhttpserver.http.header

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class HttpHeadersTest {

    @Test
    fun `getFirst should return first header value when present`() {
        val headers = ImmutableHttpHeaders(mapOf("Content-Type" to listOf("application/json", "text/html")))
        val result = headers.getFirst("Content-Type")
        assertEquals("application/json", result, "同名ヘッダーが複数ある場合は先頭要素が返ることを期待")
    }

    @Test
    fun `getFirst should return null when header does not exist`() {
        val headers = ImmutableHttpHeaders(emptyMap())
        val result = headers.getFirst("Authorization")
        assertNull(result, "存在しないヘッダーのgetFirstはnullを返すことを期待")
    }

    @Test
    fun `get should return list of header values when present`() {
        val headers = ImmutableHttpHeaders(mapOf("Accept" to listOf("text/plain", "text/html")))
        val result = headers["Accept"]
        assertEquals(listOf("text/plain", "text/html"), result, "get(インデクサ)はヘッダー値のリストを返すことを期待")
    }

    @Test
    fun `get should return null when header does not exist`() {
        val headers = ImmutableHttpHeaders(emptyMap())
        val result = headers["Authorization"]
        assertNull(result, "存在しないヘッダーのget(インデクサ)はnullを返すことを期待")
    }

    @Test
    fun `contains should return true when header exists`() {
        val headers = ImmutableHttpHeaders(mapOf("Host" to listOf("example.com")))
        assertTrue("Host" in headers, "存在するヘッダー名はcontainsでtrueになることを期待")
    }

    @Test
    fun `contains should return false when header does not exist`() {
        val headers = ImmutableHttpHeaders(emptyMap())
        assertFalse("Authorization" in headers, "存在しないヘッダー名はcontainsでfalseになることを期待")
    }

    @Test
    fun `add should append value to existing header`() {
        val headers = MutableHttpHeaders(mapOf("Cache-Control" to listOf("no-cache")))
        headers.add("Cache-Control", "no-store")
        assertEquals(
            listOf("no-cache", "no-store"),
            headers["Cache-Control"],
            "addは既存ヘッダーに値を追記することを期待"
        )
    }

    @Test
    fun `add should create a new header if it does not exist`() {
        val headers = MutableHttpHeaders(emptyMap())
        headers.add("Authorization", "Bearer token")
        assertEquals(
            listOf("Bearer token"),
            headers["Authorization"],
            "addは存在しないヘッダーを新規作成することを期待"
        )
    }

    @Test
    fun `set should overwrite existing header values`() {
        val headers = MutableHttpHeaders(mapOf("Content-Type" to listOf("text/html", "application/json")))
        headers.set("Content-Type", "application/xml")
        assertEquals(listOf("application/xml"), headers["Content-Type"], "setは既存ヘッダーの値を上書きすることを期待")
    }

    @Test
    fun `set should create a new header if it does not exist`() {
        val headers = MutableHttpHeaders(emptyMap())
        headers.set("X-Custom-Header", "custom-value")
        assertEquals(
            listOf("custom-value"),
            headers["X-Custom-Header"],
            "setは存在しないヘッダーを新規作成することを期待"
        )
    }

    @Test
    fun `remove should delete existing header`() {
        val headers = MutableHttpHeaders(mapOf("ETag" to listOf("12345")))
        headers.remove("ETag")
        assertFalse("ETag" in headers, "remove後は対象ヘッダーが存在しないことを期待")
    }

    @Test
    fun `remove should do nothing if header does not exist`() {
        val headers = MutableHttpHeaders(emptyMap())
        headers.remove("Non-Existent-Header")
        assertFalse("Non-Existent-Header" in headers, "存在しないヘッダーをremoveしても状態が変わらないことを期待")
    }

    @Test
    fun `headers should be case-insensitive`() {
        val headers = MutableHttpHeaders(mapOf("Cache-Control" to listOf("no-cache")))
        assertTrue("cache-control" in headers, "ヘッダー名の存在判定は大文字小文字を区別しないことを期待")
        assertEquals(listOf("no-cache"), headers["CACHE-CONTROL"], "取得も大文字小文字を区別しないことを期待")
    }

    @Test
    fun `headers equality`() {
        val headers1 = ImmutableHttpHeaders(
            mapOf(
                "Cache-Control" to listOf("no-cache"),
                "Content-Type" to listOf("application/json")
            )
        )
        val headers2 = ImmutableHttpHeaders(
            mapOf(
                "Cache-Control" to listOf("no-cache"),
                "Content-Type" to listOf("application/json")
            )
        )
        assertEquals(headers1, headers2, "同一内容のヘッダーはequalsで等しいことを期待")
    }

    @Test
    fun `headers inequality`() {
        val headers1 = ImmutableHttpHeaders(mapOf("Cache-Control" to listOf("no-cache")))
        val headers2 = ImmutableHttpHeaders(mapOf("Content-Type" to listOf("application/json")))
        assertNotEquals(headers1, headers2, "内容が異なるヘッダーはequalsで等しくないことを期待")
    }

    @Nested
    inner class InvalidHeaders {
        @Test
        fun `invalid initial header name`() {
            assertThrows<IllegalArgumentException> { ImmutableHttpHeaders(mapOf("Invalid Header" to listOf("value"))) }
            assertThrows<IllegalArgumentException> { MutableHttpHeaders(mapOf("Invalid Header" to listOf("value"))) }
        }

        @Test
        fun `invalid initial header value`() {
            assertThrows<IllegalArgumentException> { ImmutableHttpHeaders(mapOf("Content-Type" to listOf("Invalid Value\n"))) }
            assertThrows<IllegalArgumentException> { MutableHttpHeaders(mapOf("Content-Type" to listOf("Invalid Value\n"))) }
        }

        @Test
        fun `invalid header name add`() {
            assertThrows<IllegalArgumentException> { MutableHttpHeaders().add("Invalid Header", "value") }
        }

        @Test
        fun `invalid header value add`() {
            assertThrows<IllegalArgumentException> { MutableHttpHeaders().add("Content-Type", "Invalid Value\n") }
        }

        @Test
        fun `invalid header name set`() {
            assertThrows<IllegalArgumentException> { MutableHttpHeaders().set("Invalid Header", "value") }
        }

        @Test
        fun `invalid header value set`() {
            assertThrows<IllegalArgumentException> { MutableHttpHeaders().set("Content-Type", "Invalid Value\n") }
        }
    }
}