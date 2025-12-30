package com.github.kevinquillen.htaccess.conformance

import com.github.kevinquillen.htaccess.engine.DefaultHtaccessEngine
import com.github.kevinquillen.htaccess.engine.model.EngineInput

data class FixtureResult(
    val fixtureId: String,
    val passed: Boolean,
    val differences: List<String>,
    val kotlinUrl: String?,
    val mwlUrl: String?,
    val error: String? = null
)

class ConformanceRunner(
    private val mwlClient: MadeWithLoveClient = MadeWithLoveClient(),
    private val kotlinEngine: DefaultHtaccessEngine = DefaultHtaccessEngine()
) {

    fun runFixture(
        id: String,
        url: String,
        htaccess: String,
        serverVariables: Map<String, String> = emptyMap()
    ): FixtureResult {
        val kotlinInput = EngineInput(
            url = url,
            htaccessContent = htaccess,
            serverVariables = serverVariables
        )

        val kotlinOutput = try {
            kotlinEngine.evaluate(kotlinInput)
        } catch (e: Exception) {
            return FixtureResult(
                fixtureId = id,
                passed = false,
                differences = listOf("Kotlin engine error: ${e.message}"),
                kotlinUrl = null,
                mwlUrl = null,
                error = e.message
            )
        }

        val mwlResponse = try {
            mwlClient.test(url, htaccess, serverVariables)
        } catch (e: Exception) {
            return FixtureResult(
                fixtureId = id,
                passed = false,
                differences = listOf("MWL API error: ${e.message}"),
                kotlinUrl = kotlinOutput.outputUrl,
                mwlUrl = null,
                error = e.message
            )
        }

        val comparison = ConformanceComparator.compare(kotlinOutput, mwlResponse)

        return FixtureResult(
            fixtureId = id,
            passed = comparison.passed,
            differences = comparison.differences,
            kotlinUrl = kotlinOutput.outputUrl,
            mwlUrl = mwlResponse.output_url
        )
    }

    fun generateReport(results: List<FixtureResult>): String {
        val passed = results.count { it.passed }
        val failed = results.count { !it.passed }

        return buildString {
            appendLine("=" .repeat(60))
            appendLine("CONFORMANCE REPORT")
            appendLine("=" .repeat(60))
            appendLine()
            appendLine("Total: ${results.size} | Passed: $passed | Failed: $failed")
            appendLine("Pass rate: ${if (results.isNotEmpty()) "%.1f%%".format(passed * 100.0 / results.size) else "N/A"}")
            appendLine()

            if (failed > 0) {
                appendLine("-".repeat(60))
                appendLine("FAILURES:")
                appendLine("-".repeat(60))
                results.filter { !it.passed }.forEach { result ->
                    appendLine()
                    appendLine("Fixture: ${result.fixtureId}")
                    appendLine("  Kotlin URL: ${result.kotlinUrl}")
                    appendLine("  MWL URL: ${result.mwlUrl}")
                    result.differences.forEach { diff ->
                        appendLine("  - $diff")
                    }
                    result.error?.let { appendLine("  Error: $it") }
                }
            }

            appendLine()
            appendLine("=" .repeat(60))
        }
    }
}
