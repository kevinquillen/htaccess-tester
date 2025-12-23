package dev.kevinquillen.htaccess.http.dto

/**
 * Response DTO for the htaccess share API endpoint.
 * Matches the JSON structure returned by POST /api/share
 */
data class ShareResponseDto(
    val shareUrl: String
)
