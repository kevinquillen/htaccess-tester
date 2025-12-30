package com.github.kevinquillen.htaccess.engine.model

data class EngineOutput(
    val outputUrl: String?,
    val statusCode: Int?,
    val trace: List<TraceLine>
)
