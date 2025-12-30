package com.github.kevinquillen.htaccess.ide.toolwindow

import com.github.kevinquillen.htaccess.domain.model.TestRequest
import com.github.kevinquillen.htaccess.domain.model.TestResult
import com.github.kevinquillen.htaccess.domain.service.HtaccessLocalService
import com.github.kevinquillen.htaccess.domain.service.HtaccessTestService
import com.intellij.openapi.application.EDT
import kotlinx.coroutines.*
import javax.swing.table.DefaultTableModel

/**
 * ViewModel for the Htaccess Tester tool window.
 * Manages UI state and coordinates between the UI and the test service.
 */
class HtaccessViewModel(
    private val testService: HtaccessTestService = HtaccessLocalService()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.EDT)

    // Input state
    var url: String = ""
    var rules: String = ""
    val serverVariables: DefaultTableModel = DefaultTableModel(arrayOf("Key", "Value"), 0)

    // Output state
    var isLoading: Boolean = false
        private set
    var lastResult: TestResult? = null
        private set
    var lastError: String? = null
        private set
    var lastException: Exception? = null
        private set

    // Callbacks for UI updates
    var onStateChanged: (() -> Unit)? = null

    fun addServerVariable(key: String = "", value: String = "") {
        serverVariables.addRow(arrayOf(key, value))
        notifyStateChanged()
    }

    fun removeServerVariable(rowIndex: Int) {
        if (rowIndex >= 0 && rowIndex < serverVariables.rowCount) {
            serverVariables.removeRow(rowIndex)
            notifyStateChanged()
        }
    }

    fun getServerVariablesMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in 0 until serverVariables.rowCount) {
            val key = serverVariables.getValueAt(i, 0)?.toString()?.trim() ?: ""
            val value = serverVariables.getValueAt(i, 1)?.toString()?.trim() ?: ""
            if (key.isNotEmpty()) {
                map[key] = value
            }
        }
        return map
    }

    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (url.isBlank()) {
            errors.add("URL is required")
        } else if (!isValidUrl(url)) {
            errors.add("URL must be a valid HTTP/HTTPS URL")
        }

        if (rules.isBlank()) {
            errors.add("Rules are required")
        }

        // Check for blank keys in server variables
        for (i in 0 until serverVariables.rowCount) {
            val key = serverVariables.getValueAt(i, 0)?.toString()?.trim() ?: ""
            val value = serverVariables.getValueAt(i, 1)?.toString()?.trim() ?: ""
            if (key.isEmpty() && value.isNotEmpty()) {
                errors.add("Server variable at row ${i + 1} has a value but no key")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val trimmed = url.trim()
            trimmed.startsWith("http://") || trimmed.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }

    fun runTest(onComplete: ((TestResult?, String?) -> Unit)? = null) {
        val validation = validate()
        if (validation is ValidationResult.Invalid) {
            lastError = validation.errors.joinToString("\n")
            lastResult = null
            notifyStateChanged()
            onComplete?.invoke(null, lastError)
            return
        }

        isLoading = true
        lastError = null
        lastException = null
        notifyStateChanged()

        scope.launch {
            try {
                val request = TestRequest(
                    url = url.trim(),
                    rules = rules,
                    serverVariables = getServerVariablesMap()
                )
                val result = testService.test(request)
                lastResult = result
                lastError = null
                lastException = null
                onComplete?.invoke(result, null)
            } catch (e: Exception) {
                lastError = "Test failed: ${e.message}"
                lastException = e
                lastResult = null
                onComplete?.invoke(null, lastError)
            } finally {
                isLoading = false
                notifyStateChanged()
            }
        }
    }

    private fun notifyStateChanged() {
        onStateChanged?.invoke()
    }

    fun dispose() {
        scope.cancel()
    }

    sealed class ValidationResult {
        data object Valid : ValidationResult()
        data class Invalid(val errors: List<String>) : ValidationResult()
    }
}
