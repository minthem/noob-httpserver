package io.github.minthem.noobhttpserver.router

import io.github.minthem.noobhttpserver.http.OriginFormValidator
import io.github.minthem.noobhttpserver.http.RequestTarget
import java.util.regex.Pattern


sealed interface PathPatternMatchResult {
    data class Match(val pathParams: Map<String, String>) : PathPatternMatchResult
    object NoMatch : PathPatternMatchResult
}


internal data class PathPattern(
    private val pattern: String
) {
    private val patternRegex: Regex
    private val paramNames: List<String>

    init {
        require(isValidPathPattern(pattern)) { "Invalid path pattern: $pattern" }
        val (patReg, params) = buildRegex()
        patternRegex = patReg
        paramNames = params
    }

    fun match(target: RequestTarget): PathPatternMatchResult {
        val m = patternRegex.matchEntire(target.rawPath) ?: return PathPatternMatchResult.NoMatch

        val params = mutableMapOf<String, String>()
        for (paramName in paramNames) {
            val value = m.groups[paramName]?.value ?: continue
            params[paramName] = value
        }

        return PathPatternMatchResult.Match(params.toMap())
    }

    private fun buildRegex(): Pair<Regex, List<String>> {
        val sb = StringBuilder()
        val reg = Pattern.compile("\\{([a-zA-Z0-9_]+)}").matcher(pattern)
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

        if(lastIndex < pattern.length) {
            sb.append(Pattern.quote(pattern.substring(lastIndex)))
        }

        sb.append("/?$")

        return Regex(sb.toString()) to paramNames
    }

    private fun isValidPathPattern(pattern: String): Boolean {
        if (pattern.isEmpty() || pattern[0] != '/') return false

        val varNameReg = Regex("\\{[a-zA-Z0-9_]+}")

        val normalizePattern = pattern.replace(varNameReg, "__DUMMY__")

        // not close bracket or open bracket
        if (normalizePattern.contains("{") || normalizePattern.contains("}")) {
            return false
        }

        // validate path pattern (rfc3986)
        if (!OriginFormValidator.isValidPath(normalizePattern)) {
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

