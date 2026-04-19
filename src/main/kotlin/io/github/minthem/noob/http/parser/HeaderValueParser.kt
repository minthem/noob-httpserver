package io.github.minthem.noob.http.parser

import io.github.minthem.noob.http.message.HeaderValue
import kotlin.text.iterator

internal object HeaderValueParser {

    fun parseSingle(raw: String): HeaderValue {
        val (firstValue, _) = HeaderValueSplitter.splitFirstAndRest(raw, ',')
        val (elemFirst, elemSecond) = HeaderValueSplitter.splitFirstAndRest(firstValue, ';')

        val mainValue = if (isQuoted(elemFirst)) {
            decodeQuotedString(elemFirst)
        } else {
            elemFirst
        }

        val paramsStrings = elemSecond?.let { HeaderValueSplitter.split(it, ';') } ?: emptyList()
        val params = mutableMapOf<String, String?>()
        paramsStrings.forEach { paramString ->
            val (name, value) = parseParam(paramString)
            params[name] = value
        }

        return HeaderValue(mainValue, params)
    }

    private fun parseParam(rawPart: String): Pair<String, String?> {
        val tokenRegex = Regex("^[!#$%&'*+.^_`|~0-9a-zA-Z-]+$")
        val parameter = rawPart.trim()
        if (parameter.isEmpty()) {
            throw IllegalArgumentException("Invalid parameter name")
        }

        val (name, value) = if (parameter.contains("=")) {
            val parts = parameter.split("=", limit = 2)
            parts[0].trim() to parts[1].trim()
        } else {
            parameter to null
        }

        if (!name.matches(tokenRegex)) {
            throw IllegalArgumentException("Invalid parameter name")
        }

        val normalizedValue = when {
            value == null -> null
            isQuoted(value) -> decodeQuotedString(value)
            else -> value
        }

        return name to normalizedValue
    }

    private fun decodeQuotedString(value: String): String {
        val sb = StringBuilder()
        var isEscaped = false
        val input = value.removeSurrounding("\"")

        for (c in input) {
            if (isEscaped) {
                isEscaped = false
                sb.append(c)
            } else if (c == '\\') {
                isEscaped = true
            } else {
                sb.append(c)
            }
        }

        if (isEscaped) {
            throw IllegalArgumentException("Invalid quoted string")
        }

        return sb.toString()
    }

    private fun isQuoted(value: String): Boolean {
        return value.startsWith("\"") && value.endsWith("\"")
    }

}

internal object HeaderValueSplitter {

    fun split(input: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var target = input
        while (true) {
            val (value, rest) = splitFirstAndRest(target, delimiter)
            result.add(value)
            if (rest == null) {
                break
            }
            target = rest
        }

        return result
    }

    fun splitFirstAndRest(input: String, delimiter: Char): Pair<String, String?> {
        val result = splitDelim(input, delimiter)
        return result
    }

    private fun splitDelim(input: String, delimiter: Char): Pair<String, String?> {
        val part = StringBuilder()
        var inQuote = false
        var escaped = false

        var index = 0

        while (index < input.length) {
            val c = input[index]

            if (escaped) {
                escaped = false
            } else if (c == '\\' && inQuote) {
                if (index + 1 >= input.length) {
                    throw IllegalArgumentException("Invalid header value")
                }
                escaped = true
            } else if (c == '"') {
                inQuote = !inQuote
            } else if (c == delimiter && !inQuote) {
                val rest = input.substring(index + 1).trimStart()
                return part.toString().trim() to rest.ifEmpty { null }
            }

            part.append(c)
            index++
        }

        if (inQuote) {
            throw IllegalArgumentException("Invalid header value")
        }

        return part.toString().trim() to null
    }
}