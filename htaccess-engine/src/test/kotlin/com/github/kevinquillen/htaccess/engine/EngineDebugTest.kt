package com.github.kevinquillen.htaccess.engine

import com.github.kevinquillen.htaccess.engine.model.EngineInput
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EngineDebugTest {

    private val engine = DefaultHtaccessEngine()

    @Test
    fun `simple rule without condition`() {
        val input = EngineInput(
            url = "http://example.com/test",
            htaccessContent = """
                RewriteEngine On
                RewriteRule ^test$ /result [L]
            """.trimIndent(),
            serverVariables = emptyMap()
        )

        val output = engine.evaluate(input)
        assertEquals("http://example.com/result", output.outputUrl)
    }

    @Test
    fun `simple redirect rule`() {
        val input = EngineInput(
            url = "http://example.com/old",
            htaccessContent = """
                RewriteEngine On
                RewriteRule ^old$ /new [R=301,L]
            """.trimIndent(),
            serverVariables = emptyMap()
        )

        val output = engine.evaluate(input)
        assertEquals("http://example.com/new", output.outputUrl)
        assertEquals(301, output.statusCode)
    }

    @Test
    fun `condition met - rule should apply`() {
        val input = EngineInput(
            url = "http://example.com/page",
            htaccessContent = """
                RewriteEngine On
                RewriteCond %{HTTP_HOST} example\.com
                RewriteRule ^page$ /matched [L]
            """.trimIndent(),
            serverVariables = mapOf("HTTP_HOST" to "example.com")
        )

        val output = engine.evaluate(input)

        val traceInfo = output.trace.joinToString("\n") { line ->
            "Line ${line.lineNumber}: '${line.rawLine}' - reached=${line.wasReached}, met=${line.wasMet}, msg=${line.message}"
        }

        val condTrace = output.trace.find { it.rawLine.contains("RewriteCond") }
        val ruleTrace = output.trace.find { it.rawLine.contains("RewriteRule") }

        assertTrue(condTrace?.wasMet == true, "Condition should be met. Trace:\n$traceInfo")
        assertTrue(ruleTrace?.wasMet == true, "Rule should be met. Trace:\n$traceInfo")
        assertEquals("http://example.com/matched", output.outputUrl, "URL mismatch. Trace:\n$traceInfo")
    }

    @Test
    fun `debug force https`() {
        val input = EngineInput(
            url = "http://example.com/secure",
            htaccessContent = """
                RewriteEngine On
                RewriteCond %{HTTPS} off
                RewriteRule ^(.*)$ https://example.com/$1 [R=301,L]
            """.trimIndent(),
            serverVariables = mapOf("HTTPS" to "off")
        )

        val output = engine.evaluate(input)
        val condTrace = output.trace.find { it.rawLine.contains("RewriteCond") }
        val ruleTrace = output.trace.find { it.rawLine.contains("RewriteRule") }

        assertTrue(condTrace?.wasMet == true, "Condition should be met, trace: $condTrace")
        assertTrue(ruleTrace?.wasMet == true, "Rule should be met, trace: $ruleTrace")
        assertEquals("https://example.com/secure", output.outputUrl)
    }

    @Test
    fun `debug trailing slash`() {
        val input = EngineInput(
            url = "http://example.com/directory",
            htaccessContent = """
                RewriteEngine On
                RewriteRule ^(.+[^/])$ $1/ [R=301,L]
            """.trimIndent(),
            serverVariables = mapOf("SERVER_NAME" to "example.com")
        )

        val output = engine.evaluate(input)
        assertEquals("http://example.com/directory/", output.outputUrl)
    }

    @Test
    fun `debug cond backref`() {
        val input = EngineInput(
            url = "http://sub.example.com/page",
            htaccessContent = """
                RewriteEngine On
                RewriteCond %{HTTP_HOST} ^(.+)\.example\.com$
                RewriteRule ^(.*)$ /sites/%1/$1 [L]
            """.trimIndent(),
            serverVariables = mapOf("HTTP_HOST" to "sub.example.com")
        )

        val output = engine.evaluate(input)
        assertEquals("http://sub.example.com/sites/sub/page", output.outputUrl)
    }

    @Test
    fun `no substitution with dash`() {
        val input = EngineInput(
            url = "http://example.com/static/image.png",
            htaccessContent = """
                RewriteEngine On
                RewriteRule ^static/ - [L]
                RewriteRule ^(.*)$ /index.php?q=$1 [L]
            """.trimIndent(),
            serverVariables = mapOf("SERVER_NAME" to "example.com")
        )

        val output = engine.evaluate(input)
        assertEquals("http://example.com/static/image.png", output.outputUrl)
    }

    @Test
    fun `query string condition`() {
        val input = EngineInput(
            url = "http://example.com/page?debug=true",
            htaccessContent = """
                RewriteEngine On
                RewriteCond %{QUERY_STRING} debug=true
                RewriteRule ^(.*)$ /debug/$1 [L]
            """.trimIndent(),
            serverVariables = mapOf("SERVER_NAME" to "example.com")
        )

        val output = engine.evaluate(input)
        assertEquals("http://example.com/debug/page?debug=true", output.outputUrl)
    }
}
