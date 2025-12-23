package dev.kevinquillen.htaccess.http

import com.google.gson.Gson
import dev.kevinquillen.htaccess.http.dto.ShareResponseDto
import org.junit.Assert.*
import org.junit.Test

class ShareResponseParsingTest {

    private val gson = Gson()

    @Test
    fun `parses share response from fixture`() {
        val json = loadFixture("share_response.json")
        val dto = gson.fromJson(json, ShareResponseDto::class.java)

        assertEquals("https://htaccess.madewithlove.com/?share=abc123-test-uuid", dto.shareUrl)
    }

    @Test
    fun `parses share response with different url`() {
        val json = """{"shareUrl": "https://htaccess.madewithlove.com/?share=xyz789"}"""
        val dto = gson.fromJson(json, ShareResponseDto::class.java)

        assertEquals("https://htaccess.madewithlove.com/?share=xyz789", dto.shareUrl)
    }

    @Test
    fun `handles minimal json structure`() {
        val json = """{"shareUrl":""}"""
        val dto = gson.fromJson(json, ShareResponseDto::class.java)

        assertEquals("", dto.shareUrl)
    }

    private fun loadFixture(filename: String): String {
        return javaClass.classLoader
            .getResourceAsStream("fixtures/$filename")
            ?.bufferedReader()
            ?.readText()
            ?: throw IllegalArgumentException("Fixture not found: $filename")
    }
}
