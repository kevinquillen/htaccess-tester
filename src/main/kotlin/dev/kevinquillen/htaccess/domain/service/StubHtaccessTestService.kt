package dev.kevinquillen.htaccess.domain.service

import dev.kevinquillen.htaccess.domain.model.ResultLine
import dev.kevinquillen.htaccess.domain.model.TestRequest
import dev.kevinquillen.htaccess.domain.model.TestResult
import kotlinx.coroutines.delay

/**
 * Stub implementation that returns fake responses for UI development.
 * Will be replaced with real HTTP client in Stage 3.
 */
class StubHtaccessTestService : HtaccessTestService {

    override suspend fun test(request: TestRequest): TestResult {
        // Simulate network delay
        delay(500)

        val lines = parseRulesIntoStubResults(request.rules)
        val outputUrl = simulateRewrite(request.url, request.rules)

        val rawResponse = """
            {
              "output_url": "$outputUrl",
              "lines": ${lines.size} rules processed
            }
        """.trimIndent()

        return TestResult(
            outputUrl = outputUrl,
            outputStatusCode = if (request.rules.contains("[R=301", ignoreCase = true)) 301 else null,
            lines = lines,
            rawResponse = rawResponse
        )
    }

    private fun parseRulesIntoStubResults(rules: String): List<ResultLine> {
        return rules.lines()
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
            .mapIndexed { index, line ->
                ResultLine(
                    line = line.trim(),
                    message = "Rule ${index + 1} evaluated",
                    isMet = line.contains("RewriteRule", ignoreCase = true),
                    isValid = !line.contains("INVALID", ignoreCase = true),
                    wasReached = true
                )
            }
    }

    private fun simulateRewrite(url: String, rules: String): String {
        // Simple simulation: if there's a RewriteRule, append /rewritten
        return if (rules.contains("RewriteRule", ignoreCase = true)) {
            url.trimEnd('/') + "/rewritten"
        } else {
            url
        }
    }
}
