package com.github.kevinquillen.htaccess.engine

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File

@Serializable
data class RegexCorpus(val cases: List<RegexCase>)

@Serializable
data class RegexCase(
    val id: String,
    val pattern: String,
    val input: String,
    val matches: Boolean,
    val groups: List<String>? = null
)

class RegexCorpusTest {

    private val corpusFile = File("../fixtures/corpus/regex_cases.yaml")

    @TestFactory
    fun `regex corpus tests`(): List<DynamicTest> {
        if (!corpusFile.exists()) {
            return listOf(DynamicTest.dynamicTest("no corpus file") {
                println("Warning: No corpus file found at ${corpusFile.absolutePath}")
            })
        }

        val corpus = Yaml.default.decodeFromString(RegexCorpus.serializer(), corpusFile.readText())

        return corpus.cases.map { case ->
            DynamicTest.dynamicTest(case.id) {
                runRegexCase(case)
            }
        }
    }

    private fun runRegexCase(case: RegexCase) {
        val result = Regex(case.pattern).find(case.input)

        if (case.matches) {
            assertNotNull(result, "Pattern '${case.pattern}' should match '${case.input}'")

            case.groups?.let { expectedGroups ->
                assertEquals(
                    expectedGroups,
                    result!!.groupValues,
                    "Group values mismatch for case ${case.id}"
                )
            }
        } else {
            assertNull(result, "Pattern '${case.pattern}' should NOT match '${case.input}'")
        }
    }

    @TestFactory
    fun `case insensitive regex tests`(): List<DynamicTest> {
        val caseInsensitiveCases = listOf(
            Triple("^foo$", "FOO", true),
            Triple("^foo$", "Foo", true),
            Triple("^[a-z]+$", "ABC", true),
            Triple("^example\\.com$", "EXAMPLE.COM", true)
        )

        return caseInsensitiveCases.mapIndexed { idx, (pattern, input, shouldMatch) ->
            DynamicTest.dynamicTest("case-insensitive-$idx") {
                val result = Regex(pattern, RegexOption.IGNORE_CASE).find(input)
                if (shouldMatch) {
                    assertNotNull(result, "Pattern '$pattern' with NC should match '$input'")
                } else {
                    assertNull(result, "Pattern '$pattern' with NC should NOT match '$input'")
                }
            }
        }
    }

    @TestFactory
    fun `regex safety tests`(): List<DynamicTest> {
        return listOf(
            DynamicTest.dynamicTest("max-length-pattern") {
                val longPattern = "a".repeat(1000)
                assertDoesNotThrow { Regex(longPattern) }
            },
            DynamicTest.dynamicTest("complex-alternation") {
                val pattern = "^(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)+$"
                val result = Regex(pattern).find("abcdefghij")
                assertNotNull(result)
            },
            DynamicTest.dynamicTest("nested-groups") {
                val pattern = "^((a)(b)(c)(d)(e)(f)(g)(h)(i))$"
                val result = Regex(pattern).find("abcdefghi")
                assertNotNull(result)
                assertEquals(11, result!!.groupValues.size)
            }
        )
    }
}
