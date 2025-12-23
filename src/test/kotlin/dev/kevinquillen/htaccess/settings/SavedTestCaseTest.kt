package dev.kevinquillen.htaccess.settings

import org.junit.Assert.*
import org.junit.Test

class SavedTestCaseTest {

    @Test
    fun `creates test case with all properties`() {
        val serverVars = mutableMapOf("HTTP_HOST" to "example.com", "HTTPS" to "on")
        val testCase = SavedTestCase(
            name = "Test Case 1",
            url = "https://example.com/page",
            rules = "RewriteRule ^test$ /result [L]",
            serverVariables = serverVars
        )

        assertEquals("Test Case 1", testCase.name)
        assertEquals("https://example.com/page", testCase.url)
        assertEquals("RewriteRule ^test$ /result [L]", testCase.rules)
        assertEquals(2, testCase.serverVariables.size)
        assertEquals("example.com", testCase.serverVariables["HTTP_HOST"])
        assertEquals("on", testCase.serverVariables["HTTPS"])
    }

    @Test
    fun `creates test case with empty defaults`() {
        val testCase = SavedTestCase()

        assertEquals("", testCase.name)
        assertEquals("", testCase.url)
        assertEquals("", testCase.rules)
        assertTrue(testCase.serverVariables.isEmpty())
    }

    @Test
    fun `creates test case with no-arg constructor for serialization`() {
        val testCase = SavedTestCase()

        assertNotNull(testCase)
        assertEquals("", testCase.name)
        assertTrue(testCase.serverVariables is MutableMap)
    }

    @Test
    fun `test case properties are mutable`() {
        val testCase = SavedTestCase()

        testCase.name = "Updated Name"
        testCase.url = "https://updated.com"
        testCase.rules = "RewriteRule ^new$ /new [R=302]"
        testCase.serverVariables["NEW_VAR"] = "value"

        assertEquals("Updated Name", testCase.name)
        assertEquals("https://updated.com", testCase.url)
        assertEquals("RewriteRule ^new$ /new [R=302]", testCase.rules)
        assertEquals("value", testCase.serverVariables["NEW_VAR"])
    }

    @Test
    fun `test case equality based on all properties`() {
        val case1 = SavedTestCase(
            name = "Test",
            url = "https://example.com",
            rules = "rule",
            serverVariables = mutableMapOf("KEY" to "VALUE")
        )
        val case2 = SavedTestCase(
            name = "Test",
            url = "https://example.com",
            rules = "rule",
            serverVariables = mutableMapOf("KEY" to "VALUE")
        )

        assertEquals(case1, case2)
    }

    @Test
    fun `test case inequality with different name`() {
        val case1 = SavedTestCase(name = "Test 1")
        val case2 = SavedTestCase(name = "Test 2")

        assertNotEquals(case1, case2)
    }

    @Test
    fun `preserves multiline rules`() {
        val multilineRules = """
            RewriteEngine On
            RewriteCond %{HTTPS} off
            RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
        """.trimIndent()

        val testCase = SavedTestCase(
            name = "HTTPS Redirect",
            url = "http://example.com/page",
            rules = multilineRules
        )

        assertTrue(testCase.rules.contains("RewriteEngine On"))
        assertTrue(testCase.rules.contains("RewriteCond"))
        assertTrue(testCase.rules.contains("RewriteRule"))
    }
}
