package com.github.kevinquillen.htaccess.http.mapper

import com.github.kevinquillen.htaccess.domain.model.ResultLine
import com.github.kevinquillen.htaccess.domain.model.TestResult
import com.github.kevinquillen.htaccess.http.dto.ResultLineDto
import com.github.kevinquillen.htaccess.http.dto.TestResponseDto

/**
 * Maps HTTP DTOs to domain models.
 */
object ResponseMapper {

    fun toDomain(dto: TestResponseDto, rawJson: String): TestResult {
        return TestResult(
            outputUrl = dto.outputUrl,
            outputStatusCode = dto.outputStatusCode,
            lines = dto.lines.map { toDomain(it) },
            rawResponse = rawJson
        )
    }

    fun toDomain(dto: ResultLineDto): ResultLine {
        return ResultLine(
            line = dto.line ?: "",
            message = dto.message,
            isMet = dto.isMet ?: false,
            isValid = dto.isValid ?: true,
            wasReached = dto.wasReached ?: false,
            isSupported = dto.isSupported ?: true
        )
    }
}
