package dev.kevinquillen.htaccess.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Application-level service for htaccess tester settings.
 * Settings are persisted globally across all projects.
 */
@Service(Service.Level.APP)
@State(
    name = "HtaccessTesterSettings",
    storages = [Storage("htaccess-tester-settings.xml")]
)
class HtaccessSettingsService : PersistentStateComponent<HtaccessSettingsState> {

    private var state = HtaccessSettingsState()

    override fun getState(): HtaccessSettingsState = state

    override fun loadState(state: HtaccessSettingsState) {
        this.state = state
    }

    /**
     * Request timeout in milliseconds.
     */
    var requestTimeoutMs: Int
        get() = state.requestTimeoutMs
        set(value) {
            state.requestTimeoutMs = value.coerceIn(1000, 60000)
        }

    /**
     * Whether the first-run notice has been acknowledged.
     */
    var firstRunAcknowledged: Boolean
        get() = state.firstRunAcknowledged
        set(value) {
            state.firstRunAcknowledged = value
        }

    /**
     * Maximum retry attempts for transient server errors.
     */
    var maxRetryAttempts: Int
        get() = state.maxRetryAttempts
        set(value) {
            state.maxRetryAttempts = value.coerceIn(0, 5)
        }

    /**
     * Initial delay before first retry in milliseconds.
     */
    var retryDelayMs: Int
        get() = state.retryDelayMs
        set(value) {
            state.retryDelayMs = value.coerceIn(100, 10000)
        }

    companion object {
        fun getInstance(): HtaccessSettingsService {
            return ApplicationManager.getApplication().getService(HtaccessSettingsService::class.java)
        }
    }
}
