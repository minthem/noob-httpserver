package io.github.minthem.noob.http.message

import io.github.minthem.noob.http.util.UriDecoder
import io.github.minthem.noob.http.io.ByteReadStream
import java.io.EOFException
import kotlin.text.iterator

@ConsistentCopyVisibility
data class RequestTarget internal constructor(private val value: String) {

    private val pathQuery = value.split('?', limit = 2).let { it.first() to it.getOrNull(1) }

    val rawPath = pathQuery.first.ifEmpty { "/" }

    val rawQuery = pathQuery.second

    val decodedPath: String by lazy { UriDecoder.decodePath(rawPath) }

    val decodedQuery: Map<String, List<String>> by lazy { decodedQuery() }

    private fun decodedQuery(): Map<String, List<String>> {
        val queries = rawQuery?.split('&') ?: emptyList()

        return queries.filter { it.isNotBlank() }.map {
            val pair = it.split('=', limit = 2)
            val decKey = UriDecoder.decodeQuery(pair[0])
            val decValue = pair.getOrNull(1)?.let { v -> UriDecoder.decodeQuery(v) } ?: ""
            decKey to decValue
        }.groupBy({ it.first }, { it.second })
    }
}

internal object RequestTargetParser {

    fun parseFromStream(stream: ByteReadStream, limit: Int = 8192): RequestTarget {
        val sb = StringBuilder()
        var state = RequestTargetState.START
        var index = 0

        while (true) {
            val c = try {
                stream.next()
            } catch (_: EOFException) {
                throw IllegalArgumentException("Unexpected end of stream in request target")
            }

            state = state.next(c)
            if (state == RequestTargetState.INVALID) {
                throw IllegalArgumentException(
                    "Invalid character in request target: '${
                        c.toInt().toChar()
                    }' (hex: ${c.toString(16)}, index: $index)"
                )
            }

            if (state == RequestTargetState.END) {
                break
            }

            sb.append(c.toInt().toChar())
            index++
            if (limit < sb.length) {
                throw IllegalArgumentException("Invalid request target")
            }
        }

        return RequestTarget(sb.toString())
    }

    private enum class RequestTargetState {
        START {
            override fun next(b: Byte): RequestTargetState {
                return if (b == SLASH) PATH else INVALID
            }
        },
        PATH {
            override fun next(b: Byte): RequestTargetState {
                return if (LOOKUP_UNRESERVED[b.toInt() and 0xFF] || LOOKUP_SUB_DELIMITERS[b.toInt() and 0xFF] || b == COLON || b == AT || b == SLASH) {
                    PATH
                } else if (b == PCT) {
                    PATH_PCT1
                } else if (b == QS) {
                    QUERY
                } else if (b == SPACE) {
                    END
                } else {
                    INVALID
                }
            }
        },
        PATH_PCT1 {
            override fun next(b: Byte): RequestTargetState {
                return if (LOOKUP_HEX_DIGITS[b.toInt() and 0xFF]) PATH_PCT2 else INVALID
            }
        },
        PATH_PCT2 {
            override fun next(b: Byte): RequestTargetState {
                return if (LOOKUP_HEX_DIGITS[b.toInt() and 0xFF]) PATH else INVALID
            }
        },
        QUERY {
            override fun next(b: Byte): RequestTargetState {
                return if (LOOKUP_UNRESERVED[b.toInt() and 0xFF] || LOOKUP_SUB_DELIMITERS[b.toInt() and 0xFF] || b == COLON || b == AT || b == SLASH || b == QS) {
                    PATH
                } else if (b == PCT) {
                    QUERY_PCT1
                } else if (b == SPACE) {
                    END
                } else {
                    INVALID
                }
            }
        },
        QUERY_PCT1 {
            override fun next(b: Byte): RequestTargetState {
                return if (LOOKUP_HEX_DIGITS[b.toInt() and 0xFF]) QUERY_PCT2 else INVALID
            }
        },
        QUERY_PCT2 {
            override fun next(b: Byte): RequestTargetState {
                return if (LOOKUP_HEX_DIGITS[b.toInt() and 0xFF]) QUERY else INVALID
            }
        },
        END {
            override fun next(b: Byte): RequestTargetState {
                return this
            }
        },
        INVALID {
            override fun next(b: Byte): RequestTargetState {
                return this
            }
        }
        ;

        abstract fun next(b: Byte): RequestTargetState

        companion object {
            private const val SLASH = '/'.code.toByte()
            private const val COLON = ':'.code.toByte()
            private const val AT = '@'.code.toByte()
            private const val PCT = '%'.code.toByte()
            private const val QS = '?'.code.toByte()
            private const val SPACE = ' '.code.toByte()

            private val LOOKUP_UNRESERVED = BooleanArray(256) { false }.apply {
                for (i in 'a'..'z') this[i.code] = true
                for (i in 'A'..'Z') this[i.code] = true
                for (i in '0'..'9') this[i.code] = true
                for (i in "-._~") this[i.code] = true
            }

            private val LOOKUP_SUB_DELIMITERS = BooleanArray(256) { false }.apply {
                for (i in "!$&'()*+,;=") this[i.code] = true
            }

            private val LOOKUP_HEX_DIGITS = BooleanArray(256) { false }.apply {
                for (i in '0'..'9') this[i.code] = true
                for (i in 'a'..'f') this[i.code] = true
                for (i in 'A'..'F') this[i.code] = true
            }
        }
    }
}

