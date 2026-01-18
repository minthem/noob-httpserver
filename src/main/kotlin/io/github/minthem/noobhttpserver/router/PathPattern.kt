package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.RequestTarget
import io.github.minthem.noobhttpserver.http.UriDecoder
import java.util.regex.Pattern


internal sealed interface PathPatternMatchResult {
    data class Match(
        val pathParams: Map<String, String>,
        val remainingPath: String? = null
    ) : PathPatternMatchResult

    object NoMatch : PathPatternMatchResult
}


internal class PathPattern private constructor(
    private val patternRegex: Regex,
    private val paramNames: List<String>,
    private val isPrefix: Boolean
) {

    fun match(target: RequestTarget): PathPatternMatchResult {
        val m = if (isPrefix) {
            patternRegex.find(target.rawPath)
        } else {
            patternRegex.matchEntire(target.rawPath)
        } ?: return PathPatternMatchResult.NoMatch

        val params = mutableMapOf<String, String>()
        for (paramName in paramNames) {
            val value = m.groups[paramName]?.value ?: continue
            params[paramName] = UriDecoder.decodePath(value)
        }

        val remainingPath = if (isPrefix) {
            val remain = target.rawPath.substring(m.range.last + 1)
            if (remain.isNotEmpty() && remain[0] == '/') {
                remain
            } else {
                "/$remain"
            }
        } else null

        return PathPatternMatchResult.Match(params.toMap(), remainingPath)
    }

    companion object {
        private val VAR_NAME_REGEX = Regex("\\{([a-zA-Z0-9_]+)}")

        fun parse(pattern: String, isPrefix: Boolean = false): PathPattern {
            require(isValidPathPattern(pattern)) { "Invalid path pattern: $pattern" }

            val sb = StringBuilder()
            val reg = Pattern.compile(VAR_NAME_REGEX.pattern).matcher(pattern)
            val paramNames = mutableListOf<String>()

            var lastIndex = 0
            sb.append("^")
            while (reg.find()) {
                val staticPath = pattern.substring(lastIndex, reg.start())
                if (staticPath.isNotEmpty()) {
                    sb.append(Pattern.quote(staticPath))
                }

                val paramName = reg.group(1)
                sb.append("(?<$paramName>[^/]+)")
                paramNames.add(paramName)

                lastIndex = reg.end()
            }

            if (lastIndex < pattern.length) {
                sb.append(Pattern.quote(pattern.substring(lastIndex)))
            }

            if (!isPrefix) {
                sb.append("/?$")
            }

            return PathPattern(sb.toString().toRegex(), paramNames, isPrefix)
        }

        private fun isValidPathPattern(pattern: String): Boolean {
            if (pattern.isEmpty() || pattern[0] != '/') return false

            val normalizePattern = pattern.replace(VAR_NAME_REGEX, "__DUMMY__")

            // not close bracket or open bracket
            if (normalizePattern.contains("{") || normalizePattern.contains("}")) {
                return false
            }

            // 変数はパスセグメントの一部であってはならない
            val segments = normalizePattern.split('/')
            for (segment in segments) {
                if (segment.contains("__DUMMY__") && segment != "__DUMMY__") {
                    return false
                }
            }

            return true
        }
    }
}

