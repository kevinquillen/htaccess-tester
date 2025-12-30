package com.github.kevinquillen.htaccess.engine.fixture

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class TestFixture(
    val id: String,
    val url: String,
    val rules: String,
    val serverVariables: Map<String, String> = emptyMap(),
    val expected: ExpectedResult
)

@Serializable
data class ExpectedResult(
    val outputUrl: String?,
    val statusCode: Int? = null,
    val lines: List<ExpectedLine> = emptyList()
)

@Serializable
data class ExpectedLine(
    val contains: String? = null,
    val lineNumber: Int? = null,
    val reached: Boolean? = null,
    val met: Boolean? = null,
    val valid: Boolean? = null,
    val supported: Boolean? = null,
    val messageContains: String? = null
)

object FixtureLoader {
    private val yaml = Yaml.default

    fun loadFixture(file: File): TestFixture {
        return yaml.decodeFromString(TestFixture.serializer(), file.readText())
    }

    fun loadAllFixtures(directory: File): List<TestFixture> {
        if (!directory.exists() || !directory.isDirectory) {
            return emptyList()
        }
        return directory.listFiles { f -> f.extension == "yaml" || f.extension == "yml" }
            ?.map { loadFixture(it) }
            ?.sortedBy { it.id }
            ?: emptyList()
    }
}
