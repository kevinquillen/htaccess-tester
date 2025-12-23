package com.github.kevinquillen.htaccess.settings

/**
 * Application-level settings state for Htaccess Tester.
 * Persisted across all projects.
 */
data class HtaccessSettingsState(
    /**
     * HTTP request timeout in milliseconds.
     */
    var requestTimeoutMs: Int = DEFAULT_TIMEOUT_MS,

    /**
     * Whether the user has acknowledged the first-run notice about remote evaluation.
     */
    var firstRunAcknowledged: Boolean = false,

    /**
     * Maximum number of retry attempts for transient server errors (5xx).
     */
    var maxRetryAttempts: Int = DEFAULT_MAX_RETRIES,

    /**
     * Initial delay in milliseconds before first retry.
     */
    var retryDelayMs: Int = DEFAULT_RETRY_DELAY_MS
) {
    companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000
        const val DEFAULT_MAX_RETRIES = 2
        const val DEFAULT_RETRY_DELAY_MS = 1000
    }
}
