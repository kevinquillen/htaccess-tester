package com.github.kevinquillen.htaccess.http

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.github.kevinquillen.htaccess.domain.model.TestRequest
import com.github.kevinquillen.htaccess.domain.model.TestResult
import com.github.kevinquillen.htaccess.domain.service.HtaccessTestService
import com.github.kevinquillen.htaccess.http.dto.ErrorResponseDto
import com.github.kevinquillen.htaccess.http.dto.TestRequestDto
import com.github.kevinquillen.htaccess.http.dto.TestResponseDto
import com.github.kevinquillen.htaccess.http.mapper.ResponseMapper
import com.github.kevinquillen.htaccess.settings.HtaccessSettingsService
import com.github.kevinquillen.htaccess.settings.HtaccessSettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * HTTP client for the htaccess.madewithlove.com API.
 * Supports configurable timeouts and automatic retry with exponential backoff for transient errors.
 */
class HtaccessRemoteClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val settingsProvider: () -> HtaccessSettings = { loadSettings() }
) : HtaccessTestService {

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    private fun createClient(timeoutMs: Long): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }

    override suspend fun test(request: TestRequest): TestResult {
        return withContext(Dispatchers.IO) {
            val settings = settingsProvider()
            val client = createClient(settings.timeoutMs)

            val requestDto = TestRequestDto(
                url = request.url,
                htaccess = request.rules,
                serverVariables = request.serverVariables
            )

            val jsonBody = gson.toJson(requestDto)
            val httpRequest = Request.Builder()
                .url(baseUrl)
                .post(jsonBody.toRequestBody(jsonMediaType))
                .header("Content-Type", "application/json")
                .build()

            executeWithRetry(client, httpRequest, settings) { responseBody ->
                val responseDto = gson.fromJson(responseBody, TestResponseDto::class.java)
                ResponseMapper.toDomain(responseDto, responseBody)
            }
        }
    }

    private suspend fun <T> executeWithRetry(
        client: OkHttpClient,
        request: Request,
        settings: HtaccessSettings,
        parseResponse: (String) -> T
    ): T {
        var lastException: Exception? = null
        var attemptCount = 0
        val maxAttempts = settings.maxRetries + 1

        while (attemptCount < maxAttempts) {
            try {
                return executeRequest(client, request, parseResponse)
            } catch (e: HtaccessApiException) {
                lastException = e
                // Only retry on transient server errors (502, 503, 504)
                if (!isRetryableError(e.statusCode)) {
                    throw e
                }
                attemptCount++
                if (attemptCount < maxAttempts) {
                    val delayMs = calculateBackoffDelay(attemptCount, settings.retryDelayMs)
                    delay(delayMs)
                }
            } catch (e: IOException) {
                lastException = HtaccessApiException(
                    message = formatNetworkError(e),
                    cause = e,
                    isRetryable = e is SocketTimeoutException
                )
                // Retry on timeout errors
                if (e is SocketTimeoutException) {
                    attemptCount++
                    if (attemptCount < maxAttempts) {
                        val delayMs = calculateBackoffDelay(attemptCount, settings.retryDelayMs)
                        delay(delayMs)
                        continue
                    }
                }
                throw lastException
            }
        }

        throw lastException ?: HtaccessApiException("Unknown error after $maxAttempts attempts")
    }

    private fun <T> executeRequest(
        client: OkHttpClient,
        request: Request,
        parseResponse: (String) -> T
    ): T {
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                throw handleErrorResponse(response.code, responseBody)
            }

            return try {
                parseResponse(responseBody)
            } catch (e: JsonSyntaxException) {
                throw HtaccessApiException(
                    message = "The API response format has changed. Please update the plugin or try again later.",
                    statusCode = response.code,
                    isSchemaError = true
                )
            }
        }
    }

    private fun isRetryableError(statusCode: Int?): Boolean {
        return statusCode in RETRYABLE_STATUS_CODES
    }

    private fun calculateBackoffDelay(attempt: Int, baseDelayMs: Int): Long {
        // Exponential backoff: baseDelay * 2^(attempt-1)
        // Attempt 1: baseDelay, Attempt 2: baseDelay*2, etc.
        val multiplier = 1 shl (attempt - 1).coerceAtMost(4)
        return (baseDelayMs * multiplier).toLong().coerceAtMost(MAX_BACKOFF_MS)
    }

    private fun formatNetworkError(e: IOException): String {
        return when (e) {
            is SocketTimeoutException -> "Request timed out. The server took too long to respond."
            else -> "Network error: ${e.message ?: "Unable to connect to the server."}"
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
            400 -> HtaccessApiException(
                message = "Invalid request: $errorMessage",
                statusCode = statusCode,
                isRetryable = false
            )
            429 -> HtaccessApiException(
                message = "Rate limit exceeded. Please wait a moment before testing again.",
                statusCode = statusCode,
                isRetryable = false,
                isRateLimited = true
            )
            in 500..599 -> HtaccessApiException(
                message = "The htaccess testing service is temporarily unavailable. Please try again in a few moments.",
                statusCode = statusCode,
                isRetryable = statusCode in RETRYABLE_STATUS_CODES
            )
            else -> HtaccessApiException(
                message = "API error ($statusCode): $errorMessage",
                statusCode = statusCode,
                isRetryable = false
            )
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://htaccess.madewithlove.com/api"
        private const val MAX_BACKOFF_MS = 10_000L
        private val RETRYABLE_STATUS_CODES = setOf(502, 503, 504)

        private fun loadSettings(): HtaccessSettings {
            return try {
                val service = HtaccessSettingsService.getInstance()
                HtaccessSettings(
                    timeoutMs = service.requestTimeoutMs.toLong(),
                    maxRetries = service.maxRetryAttempts,
                    retryDelayMs = service.retryDelayMs
                )
            } catch (e: Exception) {
                // Fallback if settings service is not available (e.g., during tests)
                HtaccessSettings()
            }
        }
    }
}

/**
 * Settings for HTTP requests.
 */
data class HtaccessSettings(
    val timeoutMs: Long = HtaccessSettingsState.DEFAULT_TIMEOUT_MS.toLong(),
    val maxRetries: Int = HtaccessSettingsState.DEFAULT_MAX_RETRIES,
    val retryDelayMs: Int = HtaccessSettingsState.DEFAULT_RETRY_DELAY_MS
)

/**
 * Exception thrown when the htaccess API returns an error.
 */
class HtaccessApiException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
    val isRetryable: Boolean = false,
    val isRateLimited: Boolean = false,
    val isSchemaError: Boolean = false
) : Exception(message, cause)
