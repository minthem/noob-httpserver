package io.github.minthem.noobhttpserver.http

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HttpHeadersTest {

    @Nested
    inner class HttpHeadersCommonTest {
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
            assertEquals(
                listOf("text/plain", "text/html"),
                result,
                "get(インデクサ)はヘッダー値のリストを返すことを期待"
            )
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

        @Test
        fun `forEach should iterate over all headers`() {
            val headers = ImmutableHttpHeaders(
                mapOf(
                    "Content-Type" to listOf("application/json"),
                    "Accept" to listOf("text/html", "text/plain"),
                    "Authorization" to listOf("Bearer token")
                )
            )
            val headers2 = MutableHttpHeaders()
            headers.forEach { key, values -> headers2.addAll(key, values) }

            assertEquals(
                headers as HttpHeaders,
                headers2 as HttpHeaders,
                "forEachは全ヘッダーを順に処理することを期待"
            )
        }

        @Test
        fun `forEach should work on empty headers`() {
            val headers = ImmutableHttpHeaders(emptyMap())
            val headerList = mutableListOf<Pair<String, List<String>>>()
            headers.forEach { key, values -> headerList.add(key to values) }

            assertTrue(headerList.isEmpty(), "ヘッダーが空の場合はforEachは何も処理しないことを期待")
        }

        @Test
        fun `immutable headers should be immutable`() {
            val mutHeaders = MutableHttpHeaders(mapOf("Content-Type" to listOf("application/json")))

            val header = mutHeaders.toImmutable()
            assertEquals(mutHeaders as HttpHeaders, header, "mutableとimmutableのヘッダーが等価であることを期待")
        }

        @Test
        fun `toMutableHeaders should return a mutable copy`() {
            val immutHeaders = ImmutableHttpHeaders(mapOf("Content-Type" to listOf("application/json")))
            val mutHeaders = immutHeaders.toMutable()

            assertEquals(immutHeaders as HttpHeaders, mutHeaders as HttpHeaders, "変換前後で内容が等価であることを期待")

            // 変更しても元の ImmutableHttpHeaders に影響がないこと
            mutHeaders.add("X-New-Header", "value")
            assertFalse("X-New-Header" in immutHeaders)
        }
    }

    @Nested
    inner class MutableHeaderTest {

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
        fun `add should append value use vararg`() {
            val headers = MutableHttpHeaders()
            headers.add(
                "Content-Type" to "application/json",
                "Cache-Control" to "no-cache",
                "Content-Type" to "text/html"
            )
            assertEquals(
                listOf("no-cache"),
                headers["Cache-Control"],
            )

            assertEquals(
                listOf("application/json", "text/html"),
                headers["Content-Type"],
                "同じキーが指定されたら追加されること"
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
            assertEquals(
                listOf("application/xml"),
                headers["Content-Type"],
                "setは既存ヘッダーの値を上書きすることを期待"
            )
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
            assertEquals(listOf("no-cache"), headers["CACHE-CONTROL"], "取得も大文字小文字を区別しないことを期待(get)")
            assertEquals(
                listOf("no-cache")[0],
                headers.getFirst("CACHE-CONTROL"),
                "取得も大文字小文字を区別しないことを期待(getFirst)"
            )
        }

        @Test
        fun `addAll should append values to an existing header`() {
            val headers = MutableHttpHeaders(mapOf("Content-Type" to listOf("application/json")))
            headers.addAll("Content-Type", listOf("text/html", "text/plain"))

            assertEquals(
                listOf("application/json", "text/html", "text/plain"),
                headers["Content-Type"],
                "addAll must append values to an existing header"
            )
        }

        @Test
        fun `addAll should create a new header if it does not exist`() {
            val headers = MutableHttpHeaders()
            headers.addAll("Accept", listOf("text/html", "text/plain"))

            assertEquals(
                listOf("text/html", "text/plain"),
                headers["Accept"],
                "addAll must create a new header if it does not exist"
            )
        }

        @Test
        fun `addAll should work with normalized keys`() {
            val headers = MutableHttpHeaders()
            headers.addAll("Content-Type", listOf("application/json"))
            headers.addAll("content-type", listOf("text/plain"))

            assertEquals(
                listOf("application/json", "text/plain"),
                headers["CONTENT-TYPE"],
                "addAll must be case-insensitive and normalize header keys"
            )
        }

        @Test
        fun `addAll should handle mixed-case header names correctly`() {
            val headers = MutableHttpHeaders()
            headers.addAll("Accept", listOf("text/html"))
            headers.addAll("aCcEpT", listOf("text/plain"))

            assertEquals(
                listOf("text/html", "text/plain"),
                headers["ACCEPT"],
                "addAll must handle mixed-case header names and normalize correctly"
            )
        }
    }

    @Nested
    inner class InvalidHeaders {
        @Test
        fun `get content-type should return header value`() {
            val headers = ImmutableHttpHeaders(mapOf("Content-Type" to listOf("application/json", "text/html")))
            assertEquals(
                MediaType.parse("application/json"),
                headers.contentType,
                "get content-type should return header value"
            )
        }
        @Test
        fun `get content-type should return header value with parameter`() {
            val headers = ImmutableHttpHeaders(mapOf("Content-Type" to listOf("application/json; charset=utf-8", "text/html")))
            assertEquals(
                MediaType.parse("application/json; charset=utf-8"),
                headers.contentType,
                "get content-type should return header value"
            )
        }

        @Test
        fun `set content-type should set header value`() {
            val headers = MutableHttpHeaders()
            headers.contentType = MediaType.parse("application/json")
            assertEquals(
                listOf("application/json"),
                headers["Content-Type"],
                "set content-type should set header value"
            )
        }

        @Test
        fun `set content-type should set header value with parameter`() {
            val headers = MutableHttpHeaders()
            headers.contentType = MediaType.parse("application/json; charset=utf-8")
            assertEquals(
                listOf("application/json; charset=utf-8"),
                headers["Content-Type"],
                "set content-type should set header value"
            )
        }

        @Test
        fun `set content-type should remove header value when null`() {
            val headers = MutableHttpHeaders(mapOf("Content-Type" to listOf("application/json")))
            headers.contentType = null
            assertNull(headers["Content-Type"], "set content-type should remove header value when null")
        }
    }
}