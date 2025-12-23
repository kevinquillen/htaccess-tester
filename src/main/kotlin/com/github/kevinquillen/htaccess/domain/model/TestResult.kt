package com.github.kevinquillen.htaccess.domain.model

data class TestResult(
    val outputUrl: String?,
    val outputStatusCode: Int?,
    val lines: List<ResultLine>,
    val rawResponse: String
)
