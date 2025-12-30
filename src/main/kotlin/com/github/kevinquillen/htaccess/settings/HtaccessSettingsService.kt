package com.github.kevinquillen.htaccess.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

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

    companion object {
        fun getInstance(): HtaccessSettingsService {
            return ApplicationManager.getApplication().getService(HtaccessSettingsService::class.java)
        }
    }
}
