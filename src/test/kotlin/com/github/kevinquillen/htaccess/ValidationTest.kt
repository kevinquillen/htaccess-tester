package com.github.kevinquillen.htaccess

import com.github.kevinquillen.htaccess.ide.toolwindow.HtaccessViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ValidationTest {

    private lateinit var viewModel: HtaccessViewModel

    @Before
    fun setUp() {
        viewModel = HtaccessViewModel()
    }

    @Test
    fun `validation fails when URL is empty`() {
        viewModel.url = ""
        viewModel.rules = "RewriteRule ^test$ /test.php [L]"

        val result = viewModel.validate()

        assertTrue(result is HtaccessViewModel.ValidationResult.Invalid)
        val errors = (result as HtaccessViewModel.ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("URL is required") })
    }

    @Test
    fun `validation fails when URL is blank`() {
        viewModel.url = "   "
        viewModel.rules = "RewriteRule ^test$ /test.php [L]"

        val result = viewModel.validate()

        assertTrue(result is HtaccessViewModel.ValidationResult.Invalid)
    }

    @Test
    fun `validation fails when URL is not HTTP or HTTPS`() {
        viewModel.url = "ftp://example.com"
        viewModel.rules = "RewriteRule ^test$ /test.php [L]"

        val result = viewModel.validate()

        assertTrue(result is HtaccessViewModel.ValidationResult.Invalid)
        val errors = (result as HtaccessViewModel.ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("valid HTTP/HTTPS URL") })
    }

    @Test
    fun `validation fails when rules are empty`() {
        viewModel.url = "https://example.com"
        viewModel.rules = ""

        val result = viewModel.validate()

        assertTrue(result is HtaccessViewModel.ValidationResult.Invalid)
        val errors = (result as HtaccessViewModel.ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("Rules are required") })
    }

    @Test
    fun `validation fails when rules are blank`() {
        viewModel.url = "https://example.com"
        viewModel.rules = "   "

        val result = viewModel.validate()

        assertTrue(result is HtaccessViewModel.ValidationResult.Invalid)
    }

    @Test
    fun `validation passes with valid HTTP URL and rules`() {
        viewModel.url = "http://example.com/page"
        viewModel.rules = "RewriteRule ^test$ /test.php [L]"

        val result = viewModel.validate()

        assertTrue(result is HtaccessViewModel.ValidationResult.Valid)
    }

    @Test
    fun `validation passes with valid HTTPS URL and rules`() {
        viewModel.url = "https://example.com/page"
        viewModel.rules = "RewriteRule ^test$ /test.php [L]"

        val result = viewModel.validate()

        assertTrue(result is HtaccessViewModel.ValidationResult.Valid)
    }

    @Test
    fun `validation fails when server variable has value but no key`() {
        viewModel.url = "https://example.com"
        viewModel.rules = "RewriteRule ^test$ /test.php [L]"
        viewModel.serverVariables.addRow(arrayOf("", "some-value"))

        val result = viewModel.validate()

        assertTrue(result is HtaccessViewModel.ValidationResult.Invalid)
        val errors = (result as HtaccessViewModel.ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("has a value but no key") })
    }

    @Test
    fun `validation passes when server variable has both key and value`() {
        viewModel.url = "https://example.com"
        viewModel.rules = "RewriteRule ^test$ /test.php [L]"
        viewModel.serverVariables.addRow(arrayOf("HTTP_HOST", "example.com"))

        val result = viewModel.validate()

        assertTrue(result is HtaccessViewModel.ValidationResult.Valid)
    }

    @Test
    fun `validation passes when server variable row is completely empty`() {
        viewModel.url = "https://example.com"
        viewModel.rules = "RewriteRule ^test$ /test.php [L]"
        viewModel.serverVariables.addRow(arrayOf("", ""))

        val result = viewModel.validate()

        assertTrue(result is HtaccessViewModel.ValidationResult.Valid)
    }

    @Test
    fun `getServerVariablesMap excludes empty keys`() {
        viewModel.serverVariables.addRow(arrayOf("HTTP_HOST", "example.com"))
        viewModel.serverVariables.addRow(arrayOf("", "ignored"))
        viewModel.serverVariables.addRow(arrayOf("HTTPS", "on"))

        val map = viewModel.getServerVariablesMap()

        assertEquals(2, map.size)
        assertEquals("example.com", map["HTTP_HOST"])
        assertEquals("on", map["HTTPS"])
        assertFalse(map.containsKey(""))
    }

    @Test
    fun `getServerVariablesMap trims whitespace`() {
        viewModel.serverVariables.addRow(arrayOf("  HTTP_HOST  ", "  example.com  "))

        val map = viewModel.getServerVariablesMap()

        assertEquals(1, map.size)
        assertEquals("example.com", map["HTTP_HOST"])
    }

    @Test
    fun `addServerVariable adds row to table`() {
        assertEquals(0, viewModel.serverVariables.rowCount)

        viewModel.addServerVariable("KEY", "VALUE")

        assertEquals(1, viewModel.serverVariables.rowCount)
        assertEquals("KEY", viewModel.serverVariables.getValueAt(0, 0))
        assertEquals("VALUE", viewModel.serverVariables.getValueAt(0, 1))
    }

    @Test
    fun `removeServerVariable removes correct row`() {
        viewModel.addServerVariable("KEY1", "VALUE1")
        viewModel.addServerVariable("KEY2", "VALUE2")
        viewModel.addServerVariable("KEY3", "VALUE3")

        viewModel.removeServerVariable(1)

        assertEquals(2, viewModel.serverVariables.rowCount)
        assertEquals("KEY1", viewModel.serverVariables.getValueAt(0, 0))
        assertEquals("KEY3", viewModel.serverVariables.getValueAt(1, 0))
    }

    @Test
    fun `removeServerVariable handles invalid index gracefully`() {
        viewModel.addServerVariable("KEY", "VALUE")

        viewModel.removeServerVariable(-1)
        viewModel.removeServerVariable(5)

        assertEquals(1, viewModel.serverVariables.rowCount)
    }
}
