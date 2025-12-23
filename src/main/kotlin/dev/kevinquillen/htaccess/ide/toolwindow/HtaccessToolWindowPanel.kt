package dev.kevinquillen.htaccess.ide.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.SwingConstants

class HtaccessToolWindowPanel(private val project: Project) : JPanel(BorderLayout()) {

    init {
        val placeholderLabel = JBLabel(
            "Htaccess Tester - UI coming in Stage 2",
            SwingConstants.CENTER
        )
        placeholderLabel.border = JBUI.Borders.empty(20)
        add(placeholderLabel, BorderLayout.CENTER)
    }
}
