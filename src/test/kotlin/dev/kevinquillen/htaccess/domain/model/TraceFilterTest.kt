package dev.kevinquillen.htaccess.domain.model

import org.junit.Assert.*
import org.junit.Test

class TraceFilterTest {

    private val sampleLines = listOf(
        ResultLine(line = "RewriteEngine On", message = "Enabled", isMet = true, isValid = true, wasReached = true),
        ResultLine(line = "RewriteCond %{HTTPS} off", message = "Condition not met", isMet = false, isValid = true, wasReached = true),
        ResultLine(line = "RewriteRule ^old$ /new [R=301]", message = "Matched", isMet = true, isValid = true, wasReached = true),
        ResultLine(line = "INVALID_SYNTAX", message = "Syntax error", isMet = false, isValid = false, wasReached = true),
        ResultLine(line = "RewriteRule ^skip$ /skip [L]", message = "Not reached", isMet = false, isValid = true, wasReached = false)
    )

    @Test
    fun `ALL filter returns all lines`() {
        val filtered = TraceFilter.filter(sampleLines, TraceFilter.ALL)
        assertEquals(5, filtered.size)
        assertEquals(sampleLines, filtered)
    }

    @Test
    fun `FAILED_ONLY filter returns invalid and not met lines`() {
        val filtered = TraceFilter.filter(sampleLines, TraceFilter.FAILED_ONLY)

        // Should include: not met condition, invalid syntax, not reached (not met)
        assertEquals(3, filtered.size)
        assertTrue(filtered.any { it.line == "RewriteCond %{HTTPS} off" })
        assertTrue(filtered.any { it.line == "INVALID_SYNTAX" })
        assertTrue(filtered.any { it.line == "RewriteRule ^skip$ /skip [L]" })
    }

    @Test
    fun `REACHED_ONLY filter returns only reached lines`() {
        val filtered = TraceFilter.filter(sampleLines, TraceFilter.REACHED_ONLY)

        assertEquals(4, filtered.size)
        assertTrue(filtered.all { it.wasReached })
        assertFalse(filtered.any { it.line == "RewriteRule ^skip$ /skip [L]" })
    }

    @Test
    fun `MET_ONLY filter returns only met lines`() {
        val filtered = TraceFilter.filter(sampleLines, TraceFilter.MET_ONLY)

        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.isMet })
        assertTrue(filtered.any { it.line == "RewriteEngine On" })
        assertTrue(filtered.any { it.line == "RewriteRule ^old$ /new [R=301]" })
    }

    @Test
    fun `filter with empty list returns empty list`() {
        val filtered = TraceFilter.filter(emptyList(), TraceFilter.ALL)
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `calculateStats counts correctly`() {
        val stats = TraceFilter.calculateStats(sampleLines)

        assertEquals(5, stats.total)
        assertEquals(2, stats.met)       // RewriteEngine On, RewriteRule matched
        assertEquals(1, stats.notMet)    // RewriteCond not met (valid, reached, not met)
        assertEquals(1, stats.invalid)   // INVALID_SYNTAX
        assertEquals(1, stats.notReached) // RewriteRule skip
    }

    @Test
    fun `calculateStats with empty list returns zeros`() {
        val stats = TraceFilter.calculateStats(emptyList())

        assertEquals(0, stats.total)
        assertEquals(0, stats.met)
        assertEquals(0, stats.notMet)
        assertEquals(0, stats.invalid)
        assertEquals(0, stats.notReached)
    }

    @Test
    fun `calculateStats with all met lines`() {
        val allMet = listOf(
            ResultLine(line = "Rule1", message = null, isMet = true, isValid = true, wasReached = true),
            ResultLine(line = "Rule2", message = null, isMet = true, isValid = true, wasReached = true)
        )

        val stats = TraceFilter.calculateStats(allMet)

        assertEquals(2, stats.total)
        assertEquals(2, stats.met)
        assertEquals(0, stats.notMet)
        assertEquals(0, stats.invalid)
        assertEquals(0, stats.notReached)
    }

    @Test
    fun `generateSummary includes result URL`() {
        val result = TestResult(
            outputUrl = "https://example.com/new-page",
            outputStatusCode = 301,
            lines = sampleLines,
            rawResponse = "{}"
        )

        val summary = TraceFilter.generateSummary(result)

        assertTrue(summary.contains("https://example.com/new-page"))
        assertTrue(summary.contains("301"))
    }

    @Test
    fun `generateSummary includes stats`() {
        val result = TestResult(
            outputUrl = "https://example.com",
            outputStatusCode = null,
            lines = sampleLines,
            rawResponse = "{}"
        )

        val summary = TraceFilter.generateSummary(result)

        assertTrue(summary.contains("5 total"))
        assertTrue(summary.contains("Met: 2"))
        assertTrue(summary.contains("Invalid: 1"))
    }

    @Test
    fun `generateSummary lists invalid rules when present`() {
        val result = TestResult(
            outputUrl = "https://example.com",
            outputStatusCode = null,
            lines = sampleLines,
            rawResponse = "{}"
        )

        val summary = TraceFilter.generateSummary(result)

        assertTrue(summary.contains("Invalid rules:"))
        assertTrue(summary.contains("INVALID_SYNTAX"))
    }

    @Test
    fun `generateSummary handles null outputUrl`() {
        val result = TestResult(
            outputUrl = null,
            outputStatusCode = null,
            lines = emptyList(),
            rawResponse = "{}"
        )

        val summary = TraceFilter.generateSummary(result)

        assertTrue(summary.contains("(unchanged)"))
    }

    @Test
    fun `TraceFilter enum has correct display names`() {
        assertEquals("Show All", TraceFilter.ALL.displayName)
        assertEquals("Failed Only", TraceFilter.FAILED_ONLY.displayName)
        assertEquals("Reached Only", TraceFilter.REACHED_ONLY.displayName)
        assertEquals("Met Only", TraceFilter.MET_ONLY.displayName)
    }
}
