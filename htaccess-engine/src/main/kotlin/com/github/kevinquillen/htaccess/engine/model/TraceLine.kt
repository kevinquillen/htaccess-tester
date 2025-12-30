package com.github.kevinquillen.htaccess.engine.model

data class TraceLine(
    val rawLine: String,
    val lineNumber: Int,
    val isValid: Boolean,
    val wasReached: Boolean,
    val wasMet: Boolean,
    val isSupported: Boolean = true,
    val message: String? = null
)
