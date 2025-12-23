package dev.kevinquillen.htaccess.http.mapper

import dev.kevinquillen.htaccess.domain.model.ResultLine
import dev.kevinquillen.htaccess.domain.model.TestResult
import dev.kevinquillen.htaccess.http.dto.ResultLineDto
import dev.kevinquillen.htaccess.http.dto.TestResponseDto

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
            line = dto.line,
            message = dto.message,
            isMet = dto.isMet,
            isValid = dto.isValid,
            wasReached = dto.wasReached,
            isSupported = dto.isSupported
        )
    }
}
