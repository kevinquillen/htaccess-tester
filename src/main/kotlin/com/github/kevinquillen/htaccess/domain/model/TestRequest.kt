package com.github.kevinquillen.htaccess.domain.model

data class TestRequest(
    val url: String,
    val rules: String,
    val serverVariables: Map<String, String> = emptyMap()
)
