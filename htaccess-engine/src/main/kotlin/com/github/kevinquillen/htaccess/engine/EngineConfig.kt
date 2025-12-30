package com.github.kevinquillen.htaccess.engine

data class EngineConfig(
    val maxIterations: Int = 100,
    val maxOutputUrlLength: Int = 8192,
    val maxRulesCount: Int = 1000
) {
    init {
        require(maxIterations > 0) { "maxIterations must be positive" }
        require(maxOutputUrlLength > 0) { "maxOutputUrlLength must be positive" }
        require(maxRulesCount > 0) { "maxRulesCount must be positive" }
    }

    companion object {
        val DEFAULT = EngineConfig()
    }
}
