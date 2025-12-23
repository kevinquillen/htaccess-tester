package dev.kevinquillen.htaccess.domain.model

data class ResultLine(
    val line: String,
    val message: String?,
    val isMet: Boolean,
    val isValid: Boolean,
    val wasReached: Boolean,
    val isSupported: Boolean = true
)
