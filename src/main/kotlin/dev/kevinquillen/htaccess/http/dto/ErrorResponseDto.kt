package dev.kevinquillen.htaccess.http.dto

/**
 * Response DTO for API error responses.
 */
data class ErrorResponseDto(
    val error: String?,
    val details: String?
)
