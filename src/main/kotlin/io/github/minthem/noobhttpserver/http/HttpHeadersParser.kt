package io.github.minthem.noobhttpserver.http

import io.github.minthem.noobhttpserver.config.HttpLimitsConfig
import io.github.minthem.noobhttpserver.http.HttpHeadersState.*
import io.github.minthem.noobhttpserver.io.ByteReadStream
import java.io.EOFException

internal class HttpHeadersParser(
    private val config: HttpLimitsConfig
) {

    fun parse(stream: ByteReadStream): HttpHeaders {
        val headers = MutableHttpHeaders()
        var state = HEADER_NAME
        val buffer = StringBuilder()
        var totalBytes = 0
        var headerCount = 0

        section@ while (true) {
            header@ while (true) {
                val b = streamNext(stream)

                totalBytes++
                if (totalBytes > config.maxHeaderSectionBytes) {
                    throw IllegalArgumentException("Too many header bytes")
                }
                state = state.next(b)

                if (state == INVALID) {
                    throw IllegalArgumentException("Invalid header name")
                } else if (state == HEADER_NAME) {
                    buffer.append(b.toInt().toChar())
                    if (buffer.length > config.maxHeaderNameBytes) {
                        throw IllegalArgumentException("Too many header name bytes")
                    }
                } else if (state == HEADER_NAME_END) {
                    break@header
                } else if (state == HEADER_SECTION_END_LF) {
                    if (buffer.isNotEmpty()) {
                        throw IllegalArgumentException("Invalid header")
                    }
                    break@section
                }
            }

            val fieldName = buffer.toString()
            if (fieldName.isEmpty()) {
                throw IllegalArgumentException("Invalid header name")
            }
            buffer.clear()

            value@ while (true) {
                val b = streamNext(stream)

                totalBytes++
                if (totalBytes > config.maxHeaderSectionBytes) {
                    throw IllegalArgumentException("Too many header bytes")
                }
                state = state.next(b)

                if (state == INVALID) {
                    throw IllegalArgumentException("Invalid header value")
                } else if (state == HEADER_VALUE) {
                    buffer.append(b.toInt().toChar())
                    if (buffer.length > config.maxHeaderValueBytes) {
                        throw IllegalArgumentException("Too many header value bytes")
                    }
                } else if (state == HEADER_END_LF) {
                    break@value
                }
            }

            val fieldValue = buffer.toString().trim()
            headers.add(fieldName, fieldValue)
            buffer.clear()

            headerCount++
            if (headerCount > config.maxHeaderCount) {
                throw IllegalArgumentException("Too many headers")
            }
        }

        return headers
    }

    private fun streamNext(stream: ByteReadStream): Byte {
        return try {
            stream.next()
        } catch (_: EOFException) {
            throw IllegalArgumentException("Unexpected end of stream in headers")
        }
    }
}


private enum class HttpHeadersState {
    HEADER_NAME {
        override fun next(b: Byte): HttpHeadersState {
            return if (LOOKUP_TOKENS[b.toInt() and 0xFF]) {
                HEADER_NAME
            } else if (b == COLON) {
                HEADER_NAME_END
            } else if (b == CR) {
                HEADER_SECTION_END_CR
            } else {
                INVALID
            }
        }
    },
    HEADER_NAME_END {
        override fun next(b: Byte): HttpHeadersState {
            return if (LOOKUP_FIELD_VCHAR[b.toInt() and 0xFF] || b == SPACE || b == H_TAB) {
                HEADER_VALUE
            } else if (b == CR) {
                HEADER_END_CR
            } else {
                INVALID
            }
        }
    },
    HEADER_VALUE {
        override fun next(b: Byte): HttpHeadersState {
            return if (LOOKUP_FIELD_VCHAR[b.toInt()] || b == SPACE || b == H_TAB) {
                HEADER_VALUE
            } else if (b == CR) {
                HEADER_END_CR
            } else {
                INVALID
            }
        }
    },
    HEADER_END_CR {
        override fun next(b: Byte): HttpHeadersState {
            return if (b == LF) HEADER_END_LF else INVALID
        }
    },
    HEADER_END_LF {
        override fun next(b: Byte): HttpHeadersState {
            return if (LOOKUP_TOKENS[b.toInt() and 0xFF]) {
                HEADER_NAME
            } else if (b == CR) {
                HEADER_SECTION_END_CR
            } else {
                INVALID
            }
        }
    },
    HEADER_SECTION_END_CR {
        override fun next(b: Byte): HttpHeadersState {
            return if (b == LF) HEADER_SECTION_END_LF else INVALID
        }
    },
    HEADER_SECTION_END_LF {
        override fun next(b: Byte): HttpHeadersState {
            return this
        }
    },
    INVALID {
        override fun next(b: Byte): HttpHeadersState {
            return this
        }
    }
    ;

    abstract fun next(b: Byte): HttpHeadersState

    companion object {
        private const val SPACE = ' '.code.toByte()
        private const val H_TAB = '\t'.code.toByte()
        private const val COLON = ':'.code.toByte()
        private const val CR = '\r'.code.toByte()
        private const val LF = '\n'.code.toByte()

        private val LOOKUP_TOKENS = BooleanArray(256) { false }.apply {
            for (i in "!#$%&'*+-.^_`|~") this[i.code] = true
            for (i in '0'..'9') this[i.code] = true
            for (i in 'A'..'Z') this[i.code] = true
            for (i in 'a'..'z') this[i.code] = true
        }

        private val LOOKUP_FIELD_VCHAR = BooleanArray(256) { false }.apply {
            for (i in 0x21..0x7E) this[i] = true // visible characters
            for (i in 0x80..0xFF) this[i] = true // obs text
        }
    }
}
