package com.github.kevinquillen.htaccess.engine

import com.github.kevinquillen.htaccess.engine.model.EngineInput
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class PerformanceTest {

    @Test
    fun `evaluate large ruleset within time budget`() {
        val rules = buildString {
            appendLine("RewriteEngine On")
            repeat(50) { i ->
                appendLine("RewriteCond %{REQUEST_URI} !^/static$i/")
                appendLine("RewriteRule ^path$i/(.*)$ /rewritten$i/\$1 [L]")
            }
            appendLine("RewriteRule ^(.*)$ /index.php?q=\$1 [L,QSA]")
        }

        val engine = DefaultHtaccessEngine()
        val input = EngineInput(
            url = "http://example.com/some/path",
            htaccessContent = rules,
            serverVariables = mapOf("SERVER_NAME" to "example.com")
        )

        val timeMs = measureTimeMillis {
            val output = engine.evaluate(input)
            assertNotNull(output.outputUrl)
        }

        assertTrue(timeMs < 1000, "Evaluation should complete within 1 second, took ${timeMs}ms")
    }

    @Test
    fun `evaluate with custom config`() {
        val config = EngineConfig(
            maxIterations = 50,
            maxOutputUrlLength = 4096,
            maxRulesCount = 500
        )
        val engine = DefaultHtaccessEngine.create(config)

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
    fun `reject ruleset exceeding max rules`() {
        val config = EngineConfig(maxRulesCount = 10)
        val engine = DefaultHtaccessEngine.create(config)

        val rules = buildString {
            appendLine("RewriteEngine On")
            repeat(20) { i ->
                appendLine("RewriteRule ^rule$i$ /target$i [L]")
            }
        }

        val input = EngineInput(
            url = "http://example.com/test",
            htaccessContent = rules,
            serverVariables = emptyMap()
        )

        assertThrows(IllegalArgumentException::class.java) {
            engine.evaluate(input)
        }
    }

    @Test
    fun `concurrent evaluation is safe`() {
        val engine = DefaultHtaccessEngine()
        val input = EngineInput(
            url = "http://example.com/test",
            htaccessContent = """
                RewriteEngine On
                RewriteRule ^test$ /result [L]
            """.trimIndent(),
            serverVariables = emptyMap()
        )

        val results = (1..10).map {
            Thread {
                repeat(100) {
                    val output = engine.evaluate(input)
                    assertEquals("http://example.com/result", output.outputUrl)
                }
            }
        }

        results.forEach { it.start() }
        results.forEach { it.join() }
    }

    @Test
    fun `iteration limit prevents infinite loops`() {
        val config = EngineConfig(maxIterations = 10)
        val engine = DefaultHtaccessEngine.create(config)

        val input = EngineInput(
            url = "http://example.com/start",
            htaccessContent = """
                RewriteEngine On
                RewriteRule ^(.*)$ /$1-loop [N]
            """.trimIndent(),
            serverVariables = emptyMap()
        )

        val output = engine.evaluate(input)
        assertNotNull(output.outputUrl)
    }
}
