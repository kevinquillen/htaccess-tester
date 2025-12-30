package com.github.kevinquillen.htaccess.conformance

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Serializable
data class MadeWithLoveRequest(
    val url: String,
    val htaccess: String,
    val serverVariables: Map<String, String> = emptyMap()
)

@Serializable
data class MadeWithLoveResponse(
    val output_url: String? = null,
    val output_status_code: Int? = null,
    val lines: List<MadeWithLoveLine> = emptyList(),
    val error: String? = null
)

@Serializable
data class MadeWithLoveLine(
    val line: String,
    val message: String? = null,
    val was_met: Boolean = false,
    val is_valid: Boolean = true,
    val was_reached: Boolean = false
)

class MadeWithLoveClient(
    private val baseUrl: String = "https://htaccess.madewithlove.com/api"
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun test(url: String, htaccess: String, serverVariables: Map<String, String> = emptyMap()): MadeWithLoveResponse {
        val requestBody = buildString {
            append("url=")
            append(java.net.URLEncoder.encode(url, "UTF-8"))
            append("&htaccess=")
            append(java.net.URLEncoder.encode(htaccess, "UTF-8"))
            serverVariables.forEach { (key, value) ->
                append("&serverVariables[$key]=")
                append(java.net.URLEncoder.encode(value, "UTF-8"))
            }
        }

        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/test"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw RuntimeException("API returned status ${response.statusCode()}: ${response.body()}")
        }

        return json.decodeFromString(MadeWithLoveResponse.serializer(), response.body())
    }
}
