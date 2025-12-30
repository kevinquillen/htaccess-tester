package com.github.kevinquillen.htaccess.domain.service

import com.github.kevinquillen.htaccess.domain.model.ResultLine
import com.github.kevinquillen.htaccess.domain.model.TestRequest
import com.github.kevinquillen.htaccess.domain.model.TestResult
import com.github.kevinquillen.htaccess.engine.DefaultHtaccessEngine
import com.github.kevinquillen.htaccess.engine.model.EngineInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HtaccessLocalService(
    private val engine: DefaultHtaccessEngine = DefaultHtaccessEngine()
) : HtaccessTestService {

    override suspend fun test(request: TestRequest): TestResult {
        return withContext(Dispatchers.Default) {
            val input = EngineInput(
                url = request.url,
                htaccessContent = request.rules,
                serverVariables = request.serverVariables
            )

            val output = engine.evaluate(input)

            TestResult(
                outputUrl = output.outputUrl,
                outputStatusCode = output.statusCode,
                lines = output.trace.map { traceLine ->
                    ResultLine(
                        line = traceLine.rawLine,
                        message = traceLine.message,
                        isMet = traceLine.wasMet,
                        isValid = traceLine.isValid,
                        wasReached = traceLine.wasReached,
                        isSupported = traceLine.isSupported
                    )
                },
                rawResponse = buildRawResponse(output.outputUrl, output.statusCode, output.trace)
            )
        }
    }

    private fun buildRawResponse(
        outputUrl: String?,
        statusCode: Int?,
        trace: List<com.github.kevinquillen.htaccess.engine.model.TraceLine>
    ): String {
        return buildString {
            appendLine("{")
            appendLine("  \"output_url\": ${outputUrl?.let { "\"$it\"" } ?: "null"},")
            appendLine("  \"output_status_code\": ${statusCode ?: "null"},")
            appendLine("  \"lines\": [")
            trace.forEachIndexed { index, line ->
                append("    {")
                append("\"line\": \"${line.rawLine.replace("\"", "\\\"")}\", ")
                append("\"was_reached\": ${line.wasReached}, ")
                append("\"was_met\": ${line.wasMet}, ")
                append("\"is_valid\": ${line.isValid}")
                line.message?.let { append(", \"message\": \"${it.replace("\"", "\\\"")}\"") }
                append("}")
                if (index < trace.size - 1) append(",")
                appendLine()
            }
            appendLine("  ]")
            appendLine("}")
        }
    }
}
