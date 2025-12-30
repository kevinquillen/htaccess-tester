package com.github.kevinquillen.htaccess.engine

import com.github.kevinquillen.htaccess.engine.model.EngineOutput
import com.github.kevinquillen.htaccess.engine.model.TraceLine
import com.github.kevinquillen.htaccess.engine.parser.CondFlag
import com.github.kevinquillen.htaccess.engine.parser.Directive
import com.github.kevinquillen.htaccess.engine.parser.RuleFlag
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class RewriteEvaluator(
    private val inputUrl: String,
    private val serverVariables: Map<String, String>,
    private val maxIterations: Int = 100,
    private val maxOutputUrlLength: Int = 8192
) {

    private var currentPath: String = ""
    private var queryString: String = ""
    private var scheme: String = "http"
    private var host: String = ""
    private var port: Int = -1
    private var rewriteEngineOn: Boolean = false
    private var stopped: Boolean = false
    private var redirectStatusCode: Int? = null
    private var redirectTarget: String? = null
    private var lastRuleBackrefs: List<String> = emptyList()
    private var lastCondBackrefs: List<String> = emptyList()
    private val trace: MutableList<TraceLine> = mutableListOf()

    fun evaluate(directives: List<Directive>): EngineOutput {
        parseInputUrl()

        var iteration = 0
        var directiveIndex = 0

        while (directiveIndex < directives.size && iteration < maxIterations) {
            if (stopped) break

            val directive = directives[directiveIndex]
            val (shouldRestart, indexDelta) = processDirective(directive, directives, directiveIndex)

            if (shouldRestart) {
                directiveIndex = 0
                iteration++
            } else {
                directiveIndex += indexDelta
            }
        }

        return buildOutput()
    }

    private fun parseInputUrl() {
        try {
            val uri = URI(inputUrl)
            scheme = uri.scheme ?: "http"
            host = uri.host ?: serverVariables["SERVER_NAME"] ?: serverVariables["HTTP_HOST"] ?: "localhost"
            port = uri.port
            currentPath = uri.rawPath ?: "/"
            queryString = uri.rawQuery ?: ""

            if (currentPath.isEmpty()) {
                currentPath = "/"
            }
        } catch (e: Exception) {
            currentPath = "/"
        }
    }

    private fun processDirective(
        directive: Directive,
        allDirectives: List<Directive>,
        currentIndex: Int
    ): Pair<Boolean, Int> {
        return when (directive) {
            is Directive.RewriteEngine -> {
                processRewriteEngine(directive)
                Pair(false, 1)
            }
            is Directive.RewriteRule -> {
                if (!rewriteEngineOn) {
                    addTrace(directive, reached = false, met = false, valid = true, message = "RewriteEngine is Off")
                    return Pair(false, 1)
                }
                val conditions = collectConditions(allDirectives, currentIndex)
                processRewriteRule(directive, conditions)
            }
            is Directive.RewriteCond -> {
                Pair(false, 1)
            }
            is Directive.Comment -> {
                addTrace(directive, reached = true, met = false, valid = true)
                Pair(false, 1)
            }
            is Directive.BlankLine -> {
                Pair(false, 1)
            }
            is Directive.Unknown -> {
                addTrace(directive, reached = true, met = false, valid = false, message = directive.error)
                Pair(false, 1)
            }
        }
    }

    private fun processRewriteEngine(directive: Directive.RewriteEngine) {
        rewriteEngineOn = directive.enabled
        addTrace(directive, reached = true, met = directive.enabled, valid = true)
    }

    private fun collectConditions(directives: List<Directive>, ruleIndex: Int): List<Directive.RewriteCond> {
        val conditions = mutableListOf<Directive.RewriteCond>()
        var i = ruleIndex - 1
        while (i >= 0) {
            when (val d = directives[i]) {
                is Directive.RewriteCond -> conditions.add(0, d)
                is Directive.Comment, is Directive.BlankLine -> { }
                else -> break
            }
            i--
        }
        return conditions
    }

    private fun processRewriteRule(
        rule: Directive.RewriteRule,
        conditions: List<Directive.RewriteCond>
    ): Pair<Boolean, Int> {
        val caseInsensitive = rule.flags.any { it is RuleFlag.NC }
        val pathToMatch = if (currentPath.startsWith("/")) currentPath.substring(1) else currentPath
        val regexOptions = if (caseInsensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()

        val patternResult = try {
            Regex(rule.pattern, regexOptions).find(pathToMatch)
        } catch (e: Exception) {
            addTrace(rule, reached = true, met = false, valid = false, message = "Invalid regex: ${e.message}")
            return Pair(false, 1)
        }

        if (patternResult == null) {
            addTrace(rule, reached = true, met = false, valid = true, message = "Rule pattern did not match")
            return Pair(false, 1)
        }

        lastRuleBackrefs = patternResult.groupValues

        if (conditions.isNotEmpty()) {
            val conditionsResult = evaluateConditions(conditions)
            if (!conditionsResult) {
                addTrace(rule, reached = true, met = false, valid = true, message = "Conditions not met")
                return Pair(false, 1)
            }
        }

        val hasF = rule.flags.any { it is RuleFlag.F }
        val hasG = rule.flags.any { it is RuleFlag.G }

        if (hasF) {
            redirectStatusCode = 403
            stopped = true
            addTrace(rule, reached = true, met = true, valid = true)
            return Pair(false, 1)
        }

        if (hasG) {
            redirectStatusCode = 410
            stopped = true
            addTrace(rule, reached = true, met = true, valid = true)
            return Pair(false, 1)
        }

        applyRule(rule)
        addTrace(rule, reached = true, met = true, valid = true)

        val hasL = rule.flags.any { it is RuleFlag.L }
        val hasEND = rule.flags.any { it is RuleFlag.END }
        val hasN = rule.flags.any { it is RuleFlag.N }
        val hasR = rule.flags.any { it is RuleFlag.R }

        if (hasEND || hasR) {
            stopped = true
            return Pair(false, 1)
        }

        if (hasL) {
            stopped = true
            return Pair(false, 1)
        }

        if (hasN) {
            return Pair(true, 0)
        }

        return Pair(false, 1)
    }

    private fun evaluateConditions(conditions: List<Directive.RewriteCond>): Boolean {
        var result = true
        var orGroup = false

        for (cond in conditions) {
            val testValue = expandVariables(cond.testString)
            val condCaseInsensitive = cond.flags.contains(CondFlag.NC)
            val regexOptions = if (condCaseInsensitive) setOf(RegexOption.IGNORE_CASE) else emptySet()

            val condResult = evaluateSingleCondition(testValue, cond.pattern, regexOptions)

            val hasOr = cond.flags.contains(CondFlag.OR)
            addTrace(cond, reached = true, met = condResult, valid = true)

            if (orGroup) {
                result = result || condResult
            } else {
                if (!result) return false
                result = condResult
            }

            orGroup = hasOr
        }

        return result
    }

    private fun evaluateSingleCondition(testValue: String, pattern: String, regexOptions: Set<RegexOption>): Boolean {
        return when {
            pattern.startsWith("!=") -> {
                val compareValue = pattern.substring(2)
                testValue != compareValue
            }
            pattern.startsWith("=") -> {
                val compareValue = pattern.substring(1)
                testValue == compareValue
            }
            pattern.startsWith("!") -> {
                val actualPattern = pattern.substring(1)
                try {
                    val match = Regex(actualPattern, regexOptions).find(testValue)
                    match == null
                } catch (e: Exception) {
                    false
                }
            }
            pattern == "-f" || pattern == "-d" || pattern == "-s" || pattern == "-l" ||
            pattern == "-F" || pattern == "-U" -> {
                false
            }
            pattern.startsWith("<") || pattern.startsWith(">") -> {
                try {
                    val testNum = testValue.toIntOrNull() ?: return false
                    val compareNum = pattern.substring(1).toIntOrNull() ?: return false
                    when {
                        pattern.startsWith("<=") -> testNum <= pattern.substring(2).toInt()
                        pattern.startsWith(">=") -> testNum >= pattern.substring(2).toInt()
                        pattern.startsWith("<") -> testNum < compareNum
                        pattern.startsWith(">") -> testNum > compareNum
                        else -> false
                    }
                } catch (e: Exception) {
                    false
                }
            }
            else -> {
                try {
                    val match = Regex(pattern, regexOptions).find(testValue)
                    if (match != null) {
                        lastCondBackrefs = match.groupValues
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    private fun applyRule(rule: Directive.RewriteRule) {
        if (rule.substitution == "-") {
            return
        }

        var newPath = expandVariables(rule.substitution)
        newPath = expandBackrefs(newPath)

        if (rule.flags.any { it is RuleFlag.R }) {
            val rFlag = rule.flags.filterIsInstance<RuleFlag.R>().first()
            redirectStatusCode = rFlag.statusCode
            redirectTarget = buildRedirectTarget(newPath, rule)
        } else {
            handleQueryString(newPath, rule)
        }
    }

    private fun buildRedirectTarget(newPath: String, rule: Directive.RewriteRule): String {
        val hasQsa = rule.flags.any { it is RuleFlag.QSA }
        val hasNe = rule.flags.any { it is RuleFlag.NE }

        val isAbsolute = newPath.startsWith("http://") || newPath.startsWith("https://")

        return if (isAbsolute) {
            val uri = try { URI(newPath) } catch (e: Exception) { return newPath }
            val newQuery = buildQueryString(uri.rawQuery, hasQsa)
            buildUriString(uri.scheme, uri.host, uri.port, uri.rawPath, newQuery, hasNe)
        } else {
            val pathQuery = newPath.split("?", limit = 2)
            val path = if (pathQuery[0].startsWith("/")) pathQuery[0] else "/${pathQuery[0]}"
            val newQueryPart = if (pathQuery.size > 1) pathQuery[1] else null
            val newQuery = buildQueryString(newQueryPart, hasQsa)
            buildUriString(scheme, host, port, path, newQuery, hasNe)
        }
    }

    private fun buildQueryString(newQuery: String?, hasQsa: Boolean): String {
        return when {
            hasQsa && newQuery != null && queryString.isNotEmpty() -> "$newQuery&$queryString"
            hasQsa && newQuery == null && queryString.isNotEmpty() -> queryString
            newQuery != null -> newQuery
            else -> ""
        }
    }

    private fun buildUriString(
        scheme: String,
        host: String,
        port: Int,
        path: String,
        query: String,
        noEscape: Boolean
    ): String {
        val portPart = if (port > 0 && port != 80 && port != 443) ":$port" else ""
        val encodedPath = if (noEscape) path else encodePath(path)
        val queryPart = if (query.isNotEmpty()) "?$query" else ""
        return "$scheme://$host$portPart$encodedPath$queryPart"
    }

    private fun encodePath(path: String): String {
        return try {
            val decoded = URLDecoder.decode(path, StandardCharsets.UTF_8.name())
            URLEncoder.encode(decoded, StandardCharsets.UTF_8.name())
                .replace("%2F", "/")
                .replace("+", "%20")
        } catch (e: Exception) {
            path
        }
    }

    private fun handleQueryString(newPath: String, rule: Directive.RewriteRule) {
        val hasQsa = rule.flags.any { it is RuleFlag.QSA }
        val pathQuery = newPath.split("?", limit = 2)

        currentPath = if (pathQuery[0].startsWith("/")) pathQuery[0] else "/${pathQuery[0]}"

        val newQueryPart = if (pathQuery.size > 1) pathQuery[1] else null
        val hasQueryInSubstitution = newPath.contains("?")

        queryString = when {
            hasQsa && newQueryPart != null && queryString.isNotEmpty() -> "$newQueryPart&$queryString"
            hasQsa && newQueryPart == null -> queryString
            hasQueryInSubstitution -> newQueryPart ?: ""
            else -> queryString
        }
    }

    private fun expandVariables(input: String): String {
        var result = input
        val varPattern = Regex("%\\{([^}]+)\\}")

        varPattern.findAll(input).forEach { match ->
            val varName = match.groupValues[1]
            val value = resolveVariable(varName)
            result = result.replace(match.value, value)
        }

        return result
    }

    private fun resolveVariable(varName: String): String {
        return serverVariables[varName] ?: when (varName) {
            "REQUEST_URI" -> currentPath + (if (queryString.isNotEmpty()) "?$queryString" else "")
            "QUERY_STRING" -> queryString
            "HTTP_HOST" -> host
            "SERVER_NAME" -> host
            "HTTPS" -> if (scheme == "https") "on" else "off"
            "SERVER_PORT" -> if (port > 0) port.toString() else if (scheme == "https") "443" else "80"
            "REQUEST_FILENAME" -> currentPath
            "THE_REQUEST" -> "GET $currentPath HTTP/1.1"
            else -> ""
        }
    }

    private fun expandBackrefs(input: String): String {
        var result = input

        for (i in lastRuleBackrefs.indices) {
            result = result.replace("\$$i", lastRuleBackrefs[i])
        }

        for (i in lastCondBackrefs.indices) {
            result = result.replace("%$i", lastCondBackrefs[i])
        }

        return result
    }

    private fun addTrace(
        directive: Directive,
        reached: Boolean,
        met: Boolean,
        valid: Boolean,
        message: String? = null,
        supported: Boolean = true
    ) {
        trace.add(
            TraceLine(
                rawLine = directive.rawLine,
                lineNumber = directive.sourceLineNo,
                isValid = valid,
                wasReached = reached,
                wasMet = met,
                isSupported = supported,
                message = message
            )
        )
    }

    private fun buildOutput(): EngineOutput {
        val outputUrl = when {
            redirectTarget != null -> redirectTarget
            else -> buildCurrentUrl()
        }

        return EngineOutput(
            outputUrl = outputUrl,
            statusCode = redirectStatusCode,
            trace = trace
        )
    }

    private fun buildCurrentUrl(): String {
        val portPart = if (port > 0 && port != 80 && port != 443) ":$port" else ""
        val queryPart = if (queryString.isNotEmpty()) "?$queryString" else ""
        return "$scheme://$host$portPart$currentPath$queryPart"
    }
}
