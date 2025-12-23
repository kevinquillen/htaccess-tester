package com.github.kevinquillen.htaccess.http.dto

/**
 * Request DTO for the htaccess test API endpoint.
 * Matches the JSON structure expected by POST /api
 */
data class TestRequestDto(
    val url: String,
    val htaccess: String,
    val serverVariables: Map<String, String>
)
