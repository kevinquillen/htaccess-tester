package com.github.kevinquillen.htaccess.engine

import com.github.kevinquillen.htaccess.engine.model.EngineInput
import com.github.kevinquillen.htaccess.engine.model.EngineOutput
import com.github.kevinquillen.htaccess.engine.parser.HtaccessParser

class DefaultHtaccessEngine(
    private val config: EngineConfig = EngineConfig.DEFAULT
) : HtaccessEngine {

    override fun evaluate(input: EngineInput): EngineOutput {
        val directives = HtaccessParser.parse(input.htaccessContent)

        if (directives.size > config.maxRulesCount) {
            throw IllegalArgumentException("Rule count ${directives.size} exceeds maximum ${config.maxRulesCount}")
        }

        val evaluator = RewriteEvaluator(
            inputUrl = input.url,
            serverVariables = input.serverVariables,
            maxIterations = config.maxIterations,
            maxOutputUrlLength = config.maxOutputUrlLength
        )
        return evaluator.evaluate(directives)
    }

    companion object {
        fun create(config: EngineConfig = EngineConfig.DEFAULT): HtaccessEngine {
            return DefaultHtaccessEngine(config)
        }
    }
}
