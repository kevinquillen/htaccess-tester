package dev.kevinquillen.htaccess.http

import com.google.gson.Gson
import dev.kevinquillen.htaccess.http.dto.ResultLineDto
import dev.kevinquillen.htaccess.http.dto.TestResponseDto
import dev.kevinquillen.htaccess.http.mapper.ResponseMapper
import org.junit.Assert.*
import org.junit.Test

class ResponseMapperTest {

    private val gson = Gson()

    @Test
    fun `maps successful response with redirect`() {
        val json = loadFixture("test_response_success.json")
        val dto = gson.fromJson(json, TestResponseDto::class.java)

        val result = ResponseMapper.toDomain(dto, json)

        assertEquals("https://example.com/new-page", result.outputUrl)
        assertEquals(301, result.outputStatusCode)
        assertEquals(2, result.lines.size)
        assertEquals(json, result.rawResponse)
    }

    @Test
    fun `maps response without redirect`() {
        val json = loadFixture("test_response_no_match.json")
        val dto = gson.fromJson(json, TestResponseDto::class.java)

        val result = ResponseMapper.toDomain(dto, json)

        assertEquals("https://example.com/page", result.outputUrl)
        assertNull(result.outputStatusCode)
        assertEquals(2, result.lines.size)
    }

    @Test
    fun `maps result line with all boolean flags`() {
        val dto = ResultLineDto(
            line = "RewriteRule ^test$ /result [L]",
            message = "Rule matched",
            isMet = true,
            isValid = true,
            wasReached = true,
            isSupported = true
        )

        val result = ResponseMapper.toDomain(dto)

        assertEquals("RewriteRule ^test$ /result [L]", result.line)
        assertEquals("Rule matched", result.message)
        assertTrue(result.isMet)
        assertTrue(result.isValid)
        assertTrue(result.wasReached)
        assertTrue(result.isSupported)
    }

    @Test
    fun `maps result line with false boolean flags`() {
        val dto = ResultLineDto(
            line = "RewriteRule ^other$ /other [L]",
            message = "Rule not matched",
            isMet = false,
            isValid = true,
            wasReached = true,
            isSupported = false
        )

        val result = ResponseMapper.toDomain(dto)

        assertFalse(result.isMet)
        assertTrue(result.isValid)
        assertTrue(result.wasReached)
        assertFalse(result.isSupported)
    }

    @Test
    fun `maps empty lines list`() {
        val dto = TestResponseDto(
            outputUrl = "https://example.com",
            outputStatusCode = null,
            lines = emptyList()
        )

        val result = ResponseMapper.toDomain(dto, "{}")

        assertTrue(result.lines.isEmpty())
    }

    @Test
    fun `preserves raw json in result`() {
        val json = """{"outputUrl":"test","outputStatusCode":null,"lines":[]}"""
        val dto = TestResponseDto(
            outputUrl = "test",
            outputStatusCode = null,
            lines = emptyList()
        )

        val result = ResponseMapper.toDomain(dto, json)

        assertEquals(json, result.rawResponse)
    }

    private fun loadFixture(filename: String): String {
        return javaClass.classLoader
            .getResourceAsStream("fixtures/$filename")
            ?.bufferedReader()
            ?.readText()
            ?: throw IllegalArgumentException("Fixture not found: $filename")
    }
}
