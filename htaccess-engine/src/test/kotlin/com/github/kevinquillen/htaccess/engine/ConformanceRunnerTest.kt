package com.github.kevinquillen.htaccess.engine

import com.github.kevinquillen.htaccess.engine.fixture.ExpectedLine
import com.github.kevinquillen.htaccess.engine.fixture.FixtureLoader
import com.github.kevinquillen.htaccess.engine.fixture.TestFixture
import com.github.kevinquillen.htaccess.engine.model.EngineInput
import com.github.kevinquillen.htaccess.engine.model.EngineOutput
import com.github.kevinquillen.htaccess.engine.model.TraceLine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.File

class ConformanceRunnerTest {

    private val engine: HtaccessEngine = DefaultHtaccessEngine()

    private val fixturesDir = File("../fixtures/cases")

    @TestFactory
    fun `conformance fixtures`(): List<DynamicTest> {
        val fixtures = FixtureLoader.loadAllFixtures(fixturesDir)
        if (fixtures.isEmpty()) {
            return listOf(DynamicTest.dynamicTest("no fixtures found") {
                println("Warning: No fixtures found in ${fixturesDir.absolutePath}")
            })
        }

        return fixtures.map { fixture ->
            DynamicTest.dynamicTest(fixture.id) {
                runFixture(fixture)
            }
        }
    }

    private fun runFixture(fixture: TestFixture) {
        val input = EngineInput(
            url = fixture.url,
            htaccessContent = fixture.rules,
            serverVariables = fixture.serverVariables
        )

        val output = engine.evaluate(input)

        assertOutputUrlMatches(fixture, output)
        assertStatusCodeMatches(fixture, output)
        assertTraceMatches(fixture, output)
    }

    private fun assertOutputUrlMatches(fixture: TestFixture, output: EngineOutput) {
        if (fixture.expected.outputUrl != null) {
            assertEquals(
                fixture.expected.outputUrl,
                output.outputUrl,
                "Output URL mismatch for fixture ${fixture.id}"
            )
        }
    }

    private fun assertStatusCodeMatches(fixture: TestFixture, output: EngineOutput) {
        if (fixture.expected.statusCode != null) {
            assertEquals(
                fixture.expected.statusCode,
                output.statusCode,
                "Status code mismatch for fixture ${fixture.id}"
            )
        }
    }

    private fun assertTraceMatches(fixture: TestFixture, output: EngineOutput) {
        for (expectedLine in fixture.expected.lines) {
            val actualLine = findMatchingTraceLine(expectedLine, output.trace)
                ?: fail("Could not find trace line matching: $expectedLine in fixture ${fixture.id}")

            assertLineSemantics(expectedLine, actualLine, fixture.id)
        }
    }

    private fun findMatchingTraceLine(expected: ExpectedLine, trace: List<TraceLine>): TraceLine? {
        return trace.find { actual ->
            when {
                expected.lineNumber != null -> actual.lineNumber == expected.lineNumber
                expected.contains != null -> actual.rawLine.contains(expected.contains)
                else -> false
            }
        }
    }

    private fun assertLineSemantics(expected: ExpectedLine, actual: TraceLine, fixtureId: String) {
        expected.reached?.let {
            assertEquals(it, actual.wasReached, "reached mismatch for line '${actual.rawLine}' in $fixtureId")
        }
        expected.met?.let {
            assertEquals(it, actual.wasMet, "met mismatch for line '${actual.rawLine}' in $fixtureId")
        }
        expected.valid?.let {
            assertEquals(it, actual.isValid, "valid mismatch for line '${actual.rawLine}' in $fixtureId")
        }
        expected.supported?.let {
            assertEquals(it, actual.isSupported, "supported mismatch for line '${actual.rawLine}' in $fixtureId")
        }
        expected.messageContains?.let { substring ->
            assertTrue(
                actual.message?.contains(substring) == true,
                "message should contain '$substring' but was '${actual.message}' in $fixtureId"
            )
        }
    }
}
