package com.github.kevinquillen.htaccess.ide.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Utility functions for working with editor files.
 */
object EditorUtils {

    /**
     * Checks if the given file is an htaccess file based on its name.
     */
    fun isHtaccessFile(file: VirtualFile?): Boolean {
        if (file == null) return false
        val name = file.name.lowercase()
        return name == ".htaccess" || name.endsWith(".htaccess")
    }

    /**
     * Gets the currently active editor in the project.
     */
    fun getActiveEditor(project: Project): Editor? {
        return FileEditorManager.getInstance(project).selectedTextEditor
    }

    /**
     * Gets the virtual file for the currently active editor.
     */
    fun getActiveFile(project: Project): VirtualFile? {
        return FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
    }

    /**
     * Gets the content of the currently active .htaccess file, if one is open.
     * Returns null if no htaccess file is currently active.
     */
    fun getActiveHtaccessContent(project: Project): HtaccessEditorContent? {
        val file = getActiveFile(project) ?: return null
        if (!isHtaccessFile(file)) return null

        val editor = getActiveEditor(project) ?: return null
        val content = editor.document.text

        return HtaccessEditorContent(
            content = content,
            filePath = file.path,
            fileName = file.name
        )
    }

    /**
     * Checks if there is currently an htaccess file open in the editor.
     */
    fun hasActiveHtaccessFile(project: Project): Boolean {
        val file = getActiveFile(project) ?: return false
        return isHtaccessFile(file)
    }
}

/**
 * Represents content from an htaccess file open in the editor.
 */
data class HtaccessEditorContent(
    val content: String,
    val filePath: String,
    val fileName: String
)
