package com.github.kevinquillen.htaccess.engine

import com.github.kevinquillen.htaccess.engine.model.EngineInput
import com.github.kevinquillen.htaccess.engine.model.EngineOutput

interface HtaccessEngine {
    fun evaluate(input: EngineInput): EngineOutput
}
