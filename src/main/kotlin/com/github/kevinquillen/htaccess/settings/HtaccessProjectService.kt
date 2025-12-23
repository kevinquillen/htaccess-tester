package com.github.kevinquillen.htaccess.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

/**
 * Project-level service for persisting htaccess tester data.
 * Stores saved test cases in .idea/htaccess-tester.xml
 */
@Service(Service.Level.PROJECT)
@State(
    name = "HtaccessTester",
    storages = [Storage("htaccess-tester.xml")]
)
class HtaccessProjectService : PersistentStateComponent<HtaccessProjectState> {

    private var state = HtaccessProjectState()

    override fun getState(): HtaccessProjectState = state

    override fun loadState(state: HtaccessProjectState) {
        this.state = state
    }

    fun getSavedTestCases(): List<SavedTestCase> = state.savedTestCases.toList()

    fun saveTestCase(testCase: SavedTestCase) {
        // Remove existing case with same name if present
        state.savedTestCases.removeIf { it.name == testCase.name }
        state.savedTestCases.add(testCase)
    }

    fun deleteTestCase(name: String) {
        state.savedTestCases.removeIf { it.name == name }
    }

    fun getTestCase(name: String): SavedTestCase? {
        return state.savedTestCases.find { it.name == name }
    }

    companion object {
        fun getInstance(project: Project): HtaccessProjectService {
            return project.service()
        }
    }
}
