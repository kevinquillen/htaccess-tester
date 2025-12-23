package dev.kevinquillen.htaccess.domain.model

/**
 * Filter options for the trace list display.
 */
enum class TraceFilter(val displayName: String) {
    ALL("Show All"),
    FAILED_ONLY("Failed Only"),
    REACHED_ONLY("Reached Only"),
    MET_ONLY("Met Only");

    companion object {
        /**
         * Filters a list of ResultLines based on the selected filter.
         */
        fun filter(lines: List<ResultLine>, filter: TraceFilter): List<ResultLine> {
            return when (filter) {
                ALL -> lines
                FAILED_ONLY -> lines.filter { !it.isValid || !it.isMet }
                REACHED_ONLY -> lines.filter { it.wasReached }
                MET_ONLY -> lines.filter { it.isMet }
            }
        }

        /**
         * Generates a summary string for a test result.
         */
        fun generateSummary(result: TestResult): String {
            return buildString {
                appendLine("=== Htaccess Test Summary ===")
                appendLine()
                append("Result URL: ")
                appendLine(result.outputUrl ?: "(unchanged)")
                result.outputStatusCode?.let {
                    appendLine("HTTP Status: $it")
                }
                appendLine()

                val stats = calculateStats(result.lines)
                appendLine("Rules: ${result.lines.size} total")
                appendLine("  - Met: ${stats.met}")
                appendLine("  - Not Met: ${stats.notMet}")
                appendLine("  - Invalid: ${stats.invalid}")
                appendLine("  - Not Reached: ${stats.notReached}")

                if (stats.invalid > 0) {
                    appendLine()
                    appendLine("Invalid rules:")
                    result.lines.filter { !it.isValid }.forEach {
                        appendLine("  ✗ ${it.line}")
                        it.message?.let { msg -> appendLine("    $msg") }
                    }
                }
            }
        }

        /**
         * Calculates statistics for a list of result lines.
         */
        fun calculateStats(lines: List<ResultLine>): TraceStats {
            var met = 0
            var notMet = 0
            var invalid = 0
            var notReached = 0

            for (line in lines) {
                when {
                    !line.isValid -> invalid++
                    !line.wasReached -> notReached++
                    line.isMet -> met++
                    else -> notMet++
                }
            }

            return TraceStats(
                total = lines.size,
                met = met,
                notMet = notMet,
                invalid = invalid,
                notReached = notReached
            )
        }
    }
}

/**
 * Statistics about trace results.
 */
data class TraceStats(
    val total: Int,
    val met: Int,
    val notMet: Int,
    val invalid: Int,
    val notReached: Int
)
