package com.github.kevinquillen.htaccess.conformance

import com.charleskorn.kaml.Yaml
import com.github.kevinquillen.htaccess.engine.DefaultHtaccessEngine
import com.github.kevinquillen.htaccess.engine.model.EngineInput
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import java.io.File

@Serializable
data class TestFixture(
    val id: String,
    val url: String,
    val rules: String,
    val serverVariables: Map<String, String> = emptyMap()
)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OracleConformanceTest {

    private lateinit var mwlClient: MadeWithLoveClient
    private val kotlinEngine = DefaultHtaccessEngine()
    private val fixturesDir = File("../fixtures/cases")
    private var oracleEnabled = false

    @BeforeAll
    fun setup() {
        oracleEnabled = System.getProperty("madewithlove.cli.enabled", "false").toBoolean()
        if (oracleEnabled) {
            mwlClient = MadeWithLoveClient()
        }
    }

    @TestFactory
    fun `oracle conformance tests`(): List<DynamicTest> {
        assumeTrue(oracleEnabled, "Oracle conformance tests disabled. Enable with -Dmadewithlove.cli.enabled=true")

        val fixtures = loadFixtures()
        if (fixtures.isEmpty()) {
            return listOf(DynamicTest.dynamicTest("no fixtures found") {
                println("Warning: No fixtures found in ${fixturesDir.absolutePath}")
            })
        }

        return fixtures.map { fixture ->
            DynamicTest.dynamicTest("oracle-${fixture.id}") {
                runOracleComparison(fixture)
            }
        }
    }

    private fun loadFixtures(): List<TestFixture> {
        if (!fixturesDir.exists() || !fixturesDir.isDirectory) {
            return emptyList()
        }
        return fixturesDir.listFiles { f -> f.extension == "yaml" || f.extension == "yml" }
            ?.mapNotNull { file ->
                try {
                    val content = file.readText()
                    Yaml.default.decodeFromString(TestFixture.serializer(), content)
                } catch (e: Exception) {
                    println("Failed to load fixture ${file.name}: ${e.message}")
                    null
                }
            }
            ?.sortedBy { it.id }
            ?: emptyList()
    }

    private fun runOracleComparison(fixture: TestFixture) {
        val kotlinInput = EngineInput(
            url = fixture.url,
            htaccessContent = fixture.rules,
            serverVariables = fixture.serverVariables
        )

        val kotlinOutput = kotlinEngine.evaluate(kotlinInput)

        val mwlResponse = try {
            mwlClient.test(fixture.url, fixture.rules, fixture.serverVariables)
        } catch (e: Exception) {
            throw AssertionError("Failed to call madewithlove API for ${fixture.id}: ${e.message}", e)
        }

        val result = ConformanceComparator.compare(kotlinOutput, mwlResponse)

        if (!result.passed) {
            val message = buildString {
                appendLine("Conformance mismatch for fixture '${fixture.id}':")
                result.differences.forEach { diff ->
                    appendLine("  - $diff")
                }
                appendLine()
                appendLine("Kotlin output: ${kotlinOutput.outputUrl} (status=${kotlinOutput.statusCode})")
                appendLine("MWL output: ${mwlResponse.output_url} (status=${mwlResponse.output_status_code})")
            }
            throw AssertionError(message)
        }
    }
}
