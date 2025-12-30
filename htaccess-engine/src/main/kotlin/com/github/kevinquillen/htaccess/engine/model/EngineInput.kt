package com.github.kevinquillen.htaccess.engine.model

data class EngineInput(
    val url: String,
    val htaccessContent: String,
    val serverVariables: Map<String, String> = emptyMap()
)
