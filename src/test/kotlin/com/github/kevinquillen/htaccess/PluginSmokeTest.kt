package com.github.kevinquillen.htaccess

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.github.kevinquillen.htaccess.ide.actions.OpenHtaccessTesterAction
import com.github.kevinquillen.htaccess.ide.toolwindow.HtaccessToolWindowFactory
import com.github.kevinquillen.htaccess.ide.toolwindow.HtaccessToolWindowPanel

class PluginSmokeTest : BasePlatformTestCase() {

    fun testActionClassInstantiates() {
        val action = OpenHtaccessTesterAction()
        assertNotNull("Action should instantiate without errors", action)
    }

    fun testToolWindowFactoryInstantiates() {
        val factory = HtaccessToolWindowFactory()
        assertNotNull("ToolWindowFactory should instantiate without errors", factory)
    }

    fun testToolWindowPanelInstantiates() {
        val panel = HtaccessToolWindowPanel(project)
        assertNotNull("ToolWindowPanel should instantiate without errors", panel)
    }

    fun testActionIsRegistered() {
        val action = ActionManager.getInstance().getAction("HtaccessTester.OpenToolWindow")
        assertNotNull("Action should be registered in plugin.xml", action)
    }
}
