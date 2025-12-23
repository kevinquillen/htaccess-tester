package dev.kevinquillen.htaccess.http.dto

/**
 * Response DTO for the htaccess test API endpoint.
 * Matches the JSON structure returned by POST /api
 */
data class TestResponseDto(
    val outputUrl: String?,
    val outputStatusCode: Int?,
    val lines: List<ResultLineDto>
)

/**
 * DTO for individual rule evaluation result.
 * All fields nullable to handle unexpected API responses - Gson doesn't enforce Kotlin null-safety.
 */
data class ResultLineDto(
    val line: String?,
    val message: String?,
    val isMet: Boolean?,
    val isValid: Boolean?,
    val wasReached: Boolean?,
    val isSupported: Boolean?
)
