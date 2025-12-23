package dev.kevinquillen.htaccess.domain.model

data class TestResult(
    val outputUrl: String?,
    val lines: List<ResultLine>,
    val rawResponse: String
)
