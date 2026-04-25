package io.github.minthem.noob.http.router

internal enum class SegmentKind(
    val weight: Int,
) {
    STATIC(2),
    PARAM(1),
}

internal data class PathSpecificity(
    val segments: List<SegmentKind>,
) : Comparable<PathSpecificity> {
    override fun compareTo(other: PathSpecificity): Int {
        // 固定文字列がより早い位置に来たやつを優先
        for ((self, otherSegment) in segments.zip(other.segments)) {
            if (self == otherSegment) continue
            return self.weight.compareTo(otherSegment.weight)
        }

        // 静的セグメントが多いやつを優先
        val selfStatic = countStatic()
        val otherStatic = other.countStatic()
        if (selfStatic != otherStatic) return selfStatic.compareTo(otherStatic)

        // パラメータセグメントが少ないやつを優先
        val selfParam = countParam()
        val otherParam = other.countParam()
        if (selfParam != otherParam) return otherParam.compareTo(selfParam)

        return 0
    }

    fun append(other: PathSpecificity): PathSpecificity = PathSpecificity(segments + other.segments)

    private fun countStatic(): Int = segments.count { it == SegmentKind.STATIC }

    private fun countParam(): Int = segments.count { it == SegmentKind.PARAM }

    companion object {
        fun fromPattern(path: String): PathSpecificity {
            val segments =
                path.split('/').filter { it.isNotBlank() }.map {
                    if (it.startsWith("{") && it.endsWith("}")) SegmentKind.PARAM else SegmentKind.STATIC
                }
            return PathSpecificity(segments)
        }
    }
}
