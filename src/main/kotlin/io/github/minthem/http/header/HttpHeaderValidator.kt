package io.github.minthem.noobhttpserver.http.header

internal object HttpHeaderValidator {

    private val FIELD_NAME_REGEX = Regex("^[A-Za-z0-9!#$%&'*+-.^_`|~]+$")

    fun isValidFieldName(name: String): Boolean = FIELD_NAME_REGEX.matches(name)

    fun isValidHeaderValue(value: String): Boolean {
        if (value.isEmpty()) return true
        return value.all(::isValidHeaderValueChar) && isValidHeaderValueChar(value.last())
    }

    private fun isValidHeaderValueChar(c: Char): Boolean {
        return isWhitespaceOrHorizontalTab(c) || isFieldChar(c)
    }

    private fun isWhitespaceOrHorizontalTab(c: Char): Boolean {
        return c == ' ' || c == '\t'
    }

    private fun isFieldChar(c: Char): Boolean {
        return c.code in 0x21..0x7E || c.code in 0x80..0xFF
    }
}
