package dev.kevinquillen.htaccess.http

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import dev.kevinquillen.htaccess.domain.model.TestRequest
import dev.kevinquillen.htaccess.domain.model.TestResult
import dev.kevinquillen.htaccess.domain.service.HtaccessTestService
import dev.kevinquillen.htaccess.http.dto.ErrorResponseDto
import dev.kevinquillen.htaccess.http.dto.ShareResponseDto
import dev.kevinquillen.htaccess.http.dto.TestRequestDto
import dev.kevinquillen.htaccess.http.dto.TestResponseDto
import dev.kevinquillen.htaccess.http.mapper.ResponseMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP client for the htaccess.madewithlove.com API.
 */
class HtaccessRemoteClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS
) : HtaccessTestService {

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }

    override suspend fun test(request: TestRequest): TestResult {
        return withContext(Dispatchers.IO) {
            val requestDto = TestRequestDto(
                url = request.url,
                htaccess = request.rules,
                serverVariables = request.serverVariables
            )

            val jsonBody = gson.toJson(requestDto)
            val httpRequest = Request.Builder()
                .url("$baseUrl/")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .build()

            executeRequest(httpRequest) { responseBody ->
                val responseDto = gson.fromJson(responseBody, TestResponseDto::class.java)
                ResponseMapper.toDomain(responseDto, responseBody)
            }
        }
    }

    suspend fun share(request: TestRequest): ShareResponseDto {
        return withContext(Dispatchers.IO) {
            val requestDto = TestRequestDto(
                url = request.url,
                htaccess = request.rules,
                serverVariables = request.serverVariables
            )

            val jsonBody = gson.toJson(requestDto)
            val httpRequest = Request.Builder()
                .url("$baseUrl/share")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .build()

            executeRequest(httpRequest) { responseBody ->
                gson.fromJson(responseBody, ShareResponseDto::class.java)
            }
        }
    }

    private fun <T> executeRequest(request: Request, parseResponse: (String) -> T): T {
        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    throw handleErrorResponse(response.code, responseBody)
                }

                return try {
                    parseResponse(responseBody)
                } catch (e: JsonSyntaxException) {
                    throw HtaccessApiException(
                        "Failed to parse API response: ${e.message}",
                        response.code
                    )
                }
            }
        } catch (e: IOException) {
            throw HtaccessApiException("Network error: ${e.message}", cause = e)
        }
    }

    private fun handleErrorResponse(statusCode: Int, responseBody: String): HtaccessApiException {
        val errorMessage = try {
            val errorDto = gson.fromJson(responseBody, ErrorResponseDto::class.java)
            buildString {
                append(errorDto.error ?: "Unknown error")
                errorDto.details?.let { append(": $it") }
            }
        } catch (e: Exception) {
            responseBody.ifBlank { "HTTP $statusCode" }
        }

        return when (statusCode) {
            400 -> HtaccessApiException("Validation error: $errorMessage", statusCode)
            429 -> HtaccessApiException("Rate limit exceeded. Please wait before retrying.", statusCode)
            in 500..599 -> HtaccessApiException("Server error: $errorMessage", statusCode)
            else -> HtaccessApiException("API error ($statusCode): $errorMessage", statusCode)
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://htaccess.madewithlove.com/api"
        const val DEFAULT_TIMEOUT_MS = 10_000L
    }
}

/**
 * Exception thrown when the htaccess API returns an error.
 */
class HtaccessApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)
