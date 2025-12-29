package io.github.minthem.noobhttpserver.http


internal interface RequestTargetValidator {
    fun isValid(target: String): Boolean
}

internal object OriginFormValidator : RequestTargetValidator {
    /**
     * RFC3986準拠のバリデーション(パス, クエリ)
     */
    override fun isValid(target: String): Boolean {
        val path = target.substringBefore('?')
        val query = target.substringAfter('?', "")

        if (!isValidPath(path)) return false
        if (!isValidQuery(query)) return false

        return true
    }

    /**
     * RFC3986準拠のパスのバリデーション
     */
    internal fun isValidPath(path: String): Boolean {
        if (path.isEmpty()) return true
        if (path[0] != '/') return false

        return path.split('/').all(::isValidSegment)
    }

    private fun isValidSegment(segment: String): Boolean {
        val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val subDelims = "!$&'()*+,;="
        val hexDig = "0123456789ABCDEF"

        var i = 0
        while (i < segment.length) {
            val c = segment[i]

            when (c) {
                in unreserved, in subDelims, in ":@" -> i++
                '%' -> {
                    if (i + 2 >= segment.length) return false

                    val hd1 = segment[i + 1]
                    val hd2 = segment[i + 2]

                    if (hd1 !in hexDig || hd2 !in hexDig) return false
                    i += 3
                }

                else -> return false
            }
        }

        return true
    }

    private fun isValidQuery(query: String): Boolean {
        val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val subDelims = "!$&'()*+,;="
        val hexDig = "0123456789ABCDEFabcdef"

        var i = 0
        while (i < query.length) {
            val c = query[i]
            when (c) {
                in unreserved, in subDelims, ':', '@', '/', '?' -> i++
                '%' -> {
                    if (i + 2 >= query.length) return false
                    if (query[i + 1] !in hexDig || query[i + 2] !in hexDig) return false
                    i += 3
                }

                else -> return false
            }
        }
        return true
    }
}
