package com.github.kevinquillen.htaccess.engine.parser

object HtaccessParser {

    fun parse(content: String): List<Directive> {
        return content.lines().mapIndexed { index, line ->
            parseLine(index + 1, line)
        }
    }

    private fun parseLine(lineNo: Int, line: String): Directive {
        val trimmed = line.trim()

        return when {
            trimmed.isEmpty() -> Directive.BlankLine(lineNo, line)
            trimmed.startsWith("#") -> Directive.Comment(lineNo, line)
            trimmed.startsWith("RewriteEngine", ignoreCase = true) -> parseRewriteEngine(lineNo, line, trimmed)
            trimmed.startsWith("RewriteCond", ignoreCase = true) -> parseRewriteCond(lineNo, line, trimmed)
            trimmed.startsWith("RewriteRule", ignoreCase = true) -> parseRewriteRule(lineNo, line, trimmed)
            else -> Directive.Unknown(lineNo, line)
        }
    }

    private fun parseRewriteEngine(lineNo: Int, rawLine: String, trimmed: String): Directive {
        val parts = trimmed.split(Regex("\\s+"), limit = 2)
        if (parts.size < 2) {
            return Directive.Unknown(lineNo, rawLine, "Missing On/Off argument")
        }
        val enabled = parts[1].equals("on", ignoreCase = true)
        return Directive.RewriteEngine(lineNo, rawLine, enabled)
    }

    private fun parseRewriteCond(lineNo: Int, rawLine: String, trimmed: String): Directive {
        val withoutDirective = trimmed.removePrefix("RewriteCond").trim()
        val tokens = tokenize(withoutDirective)

        if (tokens.size < 2) {
            return Directive.Unknown(lineNo, rawLine, "Invalid RewriteCond syntax")
        }

        val testString = tokens[0]
        val patternAndFlags = tokens.drop(1)
        val flagsIndex = patternAndFlags.indexOfFirst { it.startsWith("[") && it.endsWith("]") }

        val pattern: String
        val flags: Set<CondFlag>

        if (flagsIndex >= 0) {
            pattern = patternAndFlags.subList(0, flagsIndex).joinToString(" ").ifEmpty {
                return Directive.Unknown(lineNo, rawLine, "Missing pattern")
            }
            flags = parseCondFlags(patternAndFlags[flagsIndex])
        } else {
            pattern = patternAndFlags.joinToString(" ")
            flags = emptySet()
        }

        return Directive.RewriteCond(lineNo, rawLine, testString, pattern, flags)
    }

    private fun parseRewriteRule(lineNo: Int, rawLine: String, trimmed: String): Directive {
        val withoutDirective = trimmed.removePrefix("RewriteRule").trim()
        val tokens = tokenize(withoutDirective)

        if (tokens.size < 2) {
            return Directive.Unknown(lineNo, rawLine, "Invalid RewriteRule syntax")
        }

        val pattern = tokens[0]
        val flagsIndex = tokens.indexOfLast { it.startsWith("[") && it.endsWith("]") }

        val substitution: String
        val flags: Set<RuleFlag>

        if (flagsIndex > 0) {
            substitution = tokens.subList(1, flagsIndex).joinToString(" ")
            flags = parseRuleFlags(tokens[flagsIndex])
        } else {
            substitution = tokens.drop(1).joinToString(" ")
            flags = emptySet()
        }

        return Directive.RewriteRule(lineNo, rawLine, pattern, substitution, flags)
    }

    private fun tokenize(input: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '
        var i = 0

        while (i < input.length) {
            val c = input[i]
            when {
                !inQuotes && (c == '"' || c == '\'') -> {
                    inQuotes = true
                    quoteChar = c
                }
                inQuotes && c == quoteChar -> {
                    inQuotes = false
                }
                !inQuotes && c.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                else -> {
                    current.append(c)
                }
            }
            i++
        }

        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }

        return tokens
    }

    private fun parseCondFlags(flagStr: String): Set<CondFlag> {
        val inner = flagStr.removeSurrounding("[", "]")
        return inner.split(",").mapNotNull { flag ->
            when (flag.trim().uppercase()) {
                "NC", "NOCASE" -> CondFlag.NC
                "OR", "ORNEXT" -> CondFlag.OR
                "NV", "NOVARY" -> CondFlag.NV
                else -> null
            }
        }.toSet()
    }

    private fun parseRuleFlags(flagStr: String): Set<RuleFlag> {
        val inner = flagStr.removeSurrounding("[", "]")
        return inner.split(",").map { flag ->
            val trimmed = flag.trim()
            val upper = trimmed.uppercase()
            when {
                upper == "L" || upper == "LAST" -> RuleFlag.L
                upper == "END" -> RuleFlag.END
                upper == "NC" || upper == "NOCASE" -> RuleFlag.NC
                upper == "QSA" || upper == "QSAPPEND" -> RuleFlag.QSA
                upper == "NE" || upper == "NOESCAPE" -> RuleFlag.NE
                upper == "N" || upper == "NEXT" -> RuleFlag.N
                upper == "F" || upper == "FORBIDDEN" -> RuleFlag.F
                upper == "G" || upper == "GONE" -> RuleFlag.G
                upper == "PT" || upper == "PASSTHROUGH" -> RuleFlag.PT
                upper == "R" -> RuleFlag.R()
                upper.startsWith("R=") -> {
                    val code = trimmed.substringAfter("=").toIntOrNull() ?: 302
                    RuleFlag.R(code)
                }
                else -> RuleFlag.Unknown(trimmed)
            }
        }.toSet()
    }
}
