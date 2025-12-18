package io.github.minthem.noobhttpserver.http.header

import org.junit.jupiter.api.Nested
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class HttpHeaderValidatorTest {

    @Nested
    inner class IsValidHeaderName {
        @Test
        fun `isValidFieldName should return true for valid field name with alphanumeric characters`() {
            val result = HttpHeaderValidator.isValidFieldName("ContentType")
            assertTrue(result, "英数字のみのフィールド名は有効であることを期待")
        }

        @Test
        fun `isValidFieldName should return true for valid field name with special characters`() {
            val result = HttpHeaderValidator.isValidFieldName("X-Test-Header_123")
            assertTrue(result, "許可されている記号(-, _)と英数字の組み合わせは有効であることを期待")
        }

        @Test
        fun `isValidFieldName should return false for field name containing invalid spaces`() {
            val result = HttpHeaderValidator.isValidFieldName("Invalid Header")
            assertFalse(result, "フィールド名にスペースが含まれる場合は無効であることを期待")
        }

        @Test
        fun `isValidFieldName should return false for field name with invalid characters`() {
            val result = HttpHeaderValidator.isValidFieldName("Invalid@Header")
            assertFalse(result, "許可されていない文字(@)が含まれる場合は無効であることを期待")
        }

        @Test
        fun `isValidFieldName should return false for an empty field name`() {
            val result = HttpHeaderValidator.isValidFieldName("")
            assertFalse(result, "空文字のフィールド名は無効であることを期待")
        }

        @Test
        fun `isValidFieldName should return true for valid single character field name`() {
            val result = HttpHeaderValidator.isValidFieldName("X")
            assertTrue(result, "1文字のフィールド名でも正規表現に合致すれば有効であることを期待")
        }

        @Test
        fun `isValidFieldName should return false for field name starting with whitespace`() {
            val result = HttpHeaderValidator.isValidFieldName(" Invalid")
            assertFalse(result, "先頭が空白のフィールド名は無効であることを期待")
        }

        @Test
        fun `isValidFieldName should return true for valid field name with only special characters`() {
            val result = HttpHeaderValidator.isValidFieldName("!#$%&'*+-.^_`|~")
            assertTrue(result, "許可されている記号のみで構成されたフィールド名は有効であることを期待")
        }
    }

    @Nested
    inner class IsValidHeaderValue {
        @Test
        fun `isValidHeaderValue should return true for valid header value`() {
            val result = HttpHeaderValidator.isValidHeaderValue("text/plain")
            assertTrue(result, "可視ASCII文字で構成されたヘッダー値は有効であることを期待")
        }

        @Test
        fun `isValidHeaderValue should return true for header value containing semicolon and equals`() {
            val result = HttpHeaderValidator.isValidHeaderValue("text/plain; charset=utf-8")
            assertTrue(result, "可視ASCII文字(例: ';', '=')を含むヘッダー値は有効であることを期待")
        }

        @Test
        fun `isValidHeaderValue should return true for empty header value`() {
            val result = HttpHeaderValidator.isValidHeaderValue("")
            assertTrue(result, "空文字のヘッダー値は有効(許容)であることを期待")
        }

        @Test
        fun `isValidHeaderValue should return false for header value containing LF`() {
            val result = HttpHeaderValidator.isValidHeaderValue(" \t\n")
            assertFalse(result, "LF(\\n)は許可されないためヘッダー値は無効であることを期待")
        }

        @Test
        fun `isValidHeaderValue should return true for header value containing only special characters`() {
            val result = HttpHeaderValidator.isValidHeaderValue("!#$%&'*+-.^_`|~")
            assertTrue(result, "可視ASCIIの記号のみで構成されたヘッダー値は有効であることを期待")
        }

        @Test
        fun `isValidHeaderValue should return false for header value containing invalid control characters`() {
            val result = HttpHeaderValidator.isValidHeaderValue("\u0000")
            assertFalse(result, "NULなどの制御文字は許可されないため無効であることを期待")
        }
    }

}