package com.github.kevinquillen.htaccess.conformance

import com.github.kevinquillen.htaccess.engine.model.EngineOutput
import com.github.kevinquillen.htaccess.engine.model.TraceLine

data class ConformanceResult(
    val passed: Boolean,
    val differences: List<String>
)

object ConformanceComparator {

    fun compare(
        kotlinOutput: EngineOutput,
        mwlResponse: MadeWithLoveResponse
    ): ConformanceResult {
        val differences = mutableListOf<String>()

        if (kotlinOutput.outputUrl != mwlResponse.output_url) {
            differences.add(
                "Output URL mismatch: kotlin='${kotlinOutput.outputUrl}' vs mwl='${mwlResponse.output_url}'"
            )
        }

        if (kotlinOutput.statusCode != mwlResponse.output_status_code) {
            differences.add(
                "Status code mismatch: kotlin=${kotlinOutput.statusCode} vs mwl=${mwlResponse.output_status_code}"
            )
        }

        val lineComparisons = compareTraceLines(kotlinOutput.trace, mwlResponse.lines)
        differences.addAll(lineComparisons)

        return ConformanceResult(
            passed = differences.isEmpty(),
            differences = differences
        )
    }

    private fun compareTraceLines(
        kotlinLines: List<TraceLine>,
        mwlLines: List<MadeWithLoveLine>
    ): List<String> {
        val differences = mutableListOf<String>()

        val kotlinNonBlank = kotlinLines.filter { it.rawLine.isNotBlank() }
        val mwlNonBlank = mwlLines.filter { it.line.isNotBlank() }

        if (kotlinNonBlank.size != mwlNonBlank.size) {
            differences.add(
                "Line count mismatch: kotlin=${kotlinNonBlank.size} vs mwl=${mwlNonBlank.size}"
            )
        }

        val minSize = minOf(kotlinNonBlank.size, mwlNonBlank.size)
        for (i in 0 until minSize) {
            val kt = kotlinNonBlank[i]
            val mwl = mwlNonBlank[i]

            if (kt.wasReached != mwl.was_reached) {
                differences.add(
                    "Line ${i + 1} reached mismatch: kotlin=${kt.wasReached} vs mwl=${mwl.was_reached} for '${kt.rawLine.take(50)}'"
                )
            }

            if (kt.wasMet != mwl.was_met) {
                differences.add(
                    "Line ${i + 1} met mismatch: kotlin=${kt.wasMet} vs mwl=${mwl.was_met} for '${kt.rawLine.take(50)}'"
                )
            }

            if (kt.isValid != mwl.is_valid) {
                differences.add(
                    "Line ${i + 1} valid mismatch: kotlin=${kt.isValid} vs mwl=${mwl.is_valid} for '${kt.rawLine.take(50)}'"
                )
            }
        }

        return differences
    }
}
