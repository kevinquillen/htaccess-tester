package com.github.kevinquillen.htaccess.ide.editor

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import com.github.kevinquillen.htaccess.ide.toolwindow.HtaccessToolWindowPanel

/**
 * Action to test the current .htaccess file with Htaccess Tester.
 * Available in the editor context menu when an .htaccess file is open.
 */
class RunHtaccessFromEditorAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

        // Only show action for .htaccess files
        val isHtaccess = file != null && EditorUtils.isHtaccessFile(file)
        e.presentation.isEnabledAndVisible = project != null && isHtaccess
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (!EditorUtils.isHtaccessFile(file)) return

        val content = editor.document.text
        val filePath = file.path

        // Open the tool window
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Htaccess Tester")
        toolWindow?.show {
            // Find the panel and load the file
            val contentManager = toolWindow.contentManager
            val toolWindowContent = contentManager.getContent(0)
            val panel = toolWindowContent?.component as? HtaccessToolWindowPanel
            panel?.loadFromFile(filePath, content)
        }
    }
}
