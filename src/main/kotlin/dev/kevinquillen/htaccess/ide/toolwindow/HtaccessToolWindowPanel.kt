package dev.kevinquillen.htaccess.ide.toolwindow

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.kevinquillen.htaccess.domain.model.ResultLine
import dev.kevinquillen.htaccess.ide.editor.EditorUtils
import dev.kevinquillen.htaccess.settings.HtaccessProjectService
import dev.kevinquillen.htaccess.settings.SavedTestCase
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.datatransfer.StringSelection
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer

class HtaccessToolWindowPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {

    private val viewModel = HtaccessViewModel()

    // Input components
    private val urlField = JBTextField()
    private val rulesTextArea = JBTextArea(10, 40)
    private val useEditorCheckbox = JBCheckBox("Use current .htaccess file")
    private val editorFileLabel = JBLabel("")
    private val serverVarsTable = JBTable(viewModel.serverVariables)
    private val addVarButton = JButton("+")
    private val removeVarButton = JButton("-")
    private val presetComboBox = JComboBox(arrayOf(
        "-- Add preset --",
        "HTTP_HOST",
        "REQUEST_URI",
        "QUERY_STRING",
        "REQUEST_METHOD",
        "HTTPS",
        "SERVER_NAME",
        "DOCUMENT_ROOT"
    ))

    // Action buttons
    private val testButton = JButton("Test")
    private val shareButton = JButton("Share")

    // Saved cases components
    private val savedCasesComboBox = JComboBox<String>()
    private val saveButton = JButton("Save")
    private val deleteButton = JButton("Delete")

    // Output components
    private val resultUrlLabel = JBLabel("")
    private val traceListModel = DefaultListModel<ResultLine>()
    private val traceList = JBList(traceListModel)
    private val rawResponseArea = JBTextArea(5, 40)
    private val progressBar = JProgressBar()

    init {
        buildUI()
        setupListeners()
        setupEditorListener()
        viewModel.onStateChanged = { refreshUI() }
        shareButton.toolTipText = "Share this test case and copy link to clipboard"
        refreshSavedCases()
        updateEditorState()
    }

    private fun buildUI() {
        val mainPanel = JPanel(GridBagLayout())
        mainPanel.border = JBUI.Borders.empty(10)
        val gbc = GridBagConstraints()

        // URL Field
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = JBUI.insets(0, 0, 5, 10)
        mainPanel.add(JBLabel("URL:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        urlField.toolTipText = "Enter the URL to test (e.g., https://example.com/page)"
        mainPanel.add(urlField, gbc)

        // Rules section with editor toggle
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        gbc.anchor = GridBagConstraints.NORTHWEST
        mainPanel.add(JBLabel("Rules:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        mainPanel.add(buildEditorTogglePanel(), gbc)

        // Rules TextArea
        gbc.gridx = 1
        gbc.gridy = 2
        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 1.0
        gbc.weighty = 0.4
        rulesTextArea.toolTipText = "Enter your .htaccess rules"
        rulesTextArea.lineWrap = false
        val rulesScrollPane = JBScrollPane(rulesTextArea)
        rulesScrollPane.preferredSize = Dimension(400, 150)
        mainPanel.add(rulesScrollPane, gbc)

        // Server Variables Section
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        gbc.weighty = 0.0
        gbc.anchor = GridBagConstraints.NORTHWEST
        gbc.insets = JBUI.insets(10, 0, 5, 10)
        mainPanel.add(JBLabel("Server Variables:"), gbc)

        gbc.gridx = 1
        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 1.0
        gbc.weighty = 0.2
        mainPanel.add(buildServerVarsPanel(), gbc)

        // Buttons Panel
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.insets = JBUI.insets(10, 0, 10, 0)
        mainPanel.add(buildButtonsPanel(), gbc)

        // Progress Bar
        gbc.gridy = 5
        progressBar.isIndeterminate = true
        progressBar.isVisible = false
        mainPanel.add(progressBar, gbc)

        // Output Section
        gbc.gridy = 6
        gbc.fill = GridBagConstraints.BOTH
        gbc.weighty = 0.4
        mainPanel.add(buildOutputPanel(), gbc)

        add(JBScrollPane(mainPanel), BorderLayout.CENTER)
    }

    private fun buildServerVarsPanel(): JPanel {
        val panel = JPanel(BorderLayout(5, 5))

        // Table setup
        serverVarsTable.preferredScrollableViewportSize = Dimension(400, 80)
        serverVarsTable.fillsViewportHeight = true
        val tableScrollPane = JBScrollPane(serverVarsTable)

        // Buttons panel
        val buttonsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        addVarButton.toolTipText = "Add server variable"
        removeVarButton.toolTipText = "Remove selected variable"
        presetComboBox.toolTipText = "Add a common server variable"
        buttonsPanel.add(addVarButton)
        buttonsPanel.add(removeVarButton)
        buttonsPanel.add(presetComboBox)

        panel.add(tableScrollPane, BorderLayout.CENTER)
        panel.add(buttonsPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun buildEditorTogglePanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))

        useEditorCheckbox.toolTipText = "Read rules from the currently open .htaccess file in the editor"
        panel.add(useEditorCheckbox)

        editorFileLabel.foreground = JBColor.GRAY
        panel.add(editorFileLabel)

        return panel
    }

    private fun buildButtonsPanel(): JPanel {
        val panel = JPanel(BorderLayout(10, 0))

        // Test and Share buttons on the left
        val actionPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0))
        testButton.toolTipText = "Test the URL against the rules"
        actionPanel.add(testButton)
        actionPanel.add(shareButton)
        panel.add(actionPanel, BorderLayout.WEST)

        // Saved cases on the right
        val savedCasesPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0))
        savedCasesPanel.add(JBLabel("Saved:"))
        savedCasesComboBox.preferredSize = Dimension(150, savedCasesComboBox.preferredSize.height)
        savedCasesComboBox.toolTipText = "Load a saved test case"
        savedCasesPanel.add(savedCasesComboBox)
        saveButton.toolTipText = "Save current test case"
        savedCasesPanel.add(saveButton)
        deleteButton.toolTipText = "Delete selected saved case"
        savedCasesPanel.add(deleteButton)
        panel.add(savedCasesPanel, BorderLayout.EAST)

        return panel
    }

    private fun buildOutputPanel(): JPanel {
        val panel = JPanel(BorderLayout(0, 10))
        panel.border = JBUI.Borders.emptyTop(10)

        // Result URL
        val resultPanel = JPanel(BorderLayout(5, 0))
        resultPanel.add(JBLabel("Result URL:"), BorderLayout.WEST)
        resultUrlLabel.font = resultUrlLabel.font.deriveFont(java.awt.Font.BOLD)
        resultPanel.add(resultUrlLabel, BorderLayout.CENTER)
        panel.add(resultPanel, BorderLayout.NORTH)

        // Trace list with custom renderer
        traceList.cellRenderer = ResultLineCellRenderer()
        traceList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val traceScrollPane = JBScrollPane(traceList)
        traceScrollPane.border = BorderFactory.createTitledBorder("Trace")
        traceScrollPane.preferredSize = Dimension(400, 120)

        // Raw response (collapsible)
        rawResponseArea.isEditable = false
        rawResponseArea.lineWrap = true
        rawResponseArea.wrapStyleWord = true
        val rawScrollPane = JBScrollPane(rawResponseArea)
        rawScrollPane.border = BorderFactory.createTitledBorder("Raw Response")
        rawScrollPane.preferredSize = Dimension(400, 80)

        // Split pane for trace and raw
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, traceScrollPane, rawScrollPane)
        splitPane.resizeWeight = 0.7
        panel.add(splitPane, BorderLayout.CENTER)

        return panel
    }

    private fun setupListeners() {
        // URL field updates
        urlField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) { viewModel.url = urlField.text }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) { viewModel.url = urlField.text }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) { viewModel.url = urlField.text }
        })

        // Rules text area updates
        rulesTextArea.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) { viewModel.rules = rulesTextArea.text }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) { viewModel.rules = rulesTextArea.text }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) { viewModel.rules = rulesTextArea.text }
        })

        // Server variable buttons
        addVarButton.addActionListener { viewModel.addServerVariable() }
        removeVarButton.addActionListener {
            val selectedRow = serverVarsTable.selectedRow
            if (selectedRow >= 0) {
                viewModel.removeServerVariable(selectedRow)
            }
        }

        // Preset combo box
        presetComboBox.addActionListener {
            val selected = presetComboBox.selectedItem?.toString()
            if (selected != null && selected != "-- Add preset --") {
                viewModel.addServerVariable(selected, "")
                presetComboBox.selectedIndex = 0
            }
        }

        // Use editor checkbox
        useEditorCheckbox.addActionListener {
            updateEditorState()
            if (useEditorCheckbox.isSelected) {
                syncFromEditor()
            }
        }

        // Test button
        testButton.addActionListener { runTest() }

        // Share button
        shareButton.addActionListener { runShare() }

        // Saved cases
        savedCasesComboBox.addActionListener { loadSelectedCase() }
        saveButton.addActionListener { saveCurrentCase() }
        deleteButton.addActionListener { deleteSelectedCase() }
    }

    private fun runTest() {
        val validation = viewModel.validate()
        if (validation is HtaccessViewModel.ValidationResult.Invalid) {
            Messages.showErrorDialog(
                project,
                validation.errors.joinToString("\n"),
                "Validation Error"
            )
            return
        }

        viewModel.runTest { _, error ->
            if (error != null) {
                Messages.showErrorDialog(project, error, "Test Failed")
            }
        }
    }

    private fun runShare() {
        val validation = viewModel.validate()
        if (validation is HtaccessViewModel.ValidationResult.Invalid) {
            Messages.showErrorDialog(
                project,
                validation.errors.joinToString("\n"),
                "Validation Error"
            )
            return
        }

        viewModel.runShare { result, error ->
            if (error != null) {
                Messages.showErrorDialog(project, error, "Share Failed")
            } else if (result != null) {
                // Copy to clipboard
                CopyPasteManager.getInstance().setContents(StringSelection(result.shareUrl))

                // Show notification
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Htaccess Tester")
                    .createNotification(
                        "Share link copied to clipboard",
                        result.shareUrl,
                        NotificationType.INFORMATION
                    )
                    .notify(project)
            }
        }
    }

    private fun refreshUI() {
        // Update progress bar and buttons
        progressBar.isVisible = viewModel.isLoading
        testButton.isEnabled = !viewModel.isLoading
        shareButton.isEnabled = !viewModel.isLoading

        // Update results
        val result = viewModel.lastResult
        if (result != null) {
            val urlText = result.outputUrl ?: "(no output URL)"
            val statusText = result.outputStatusCode?.let { " (HTTP $it)" } ?: ""
            resultUrlLabel.text = "$urlText$statusText"

            traceListModel.clear()
            result.lines.forEach { traceListModel.addElement(it) }

            rawResponseArea.text = result.rawResponse
        } else {
            resultUrlLabel.text = ""
            traceListModel.clear()
            rawResponseArea.text = viewModel.lastError ?: ""
        }
    }

    private fun refreshSavedCases() {
        val service = HtaccessProjectService.getInstance(project)
        val cases = service.getSavedTestCases()

        savedCasesComboBox.removeAllItems()
        savedCasesComboBox.addItem("-- Select --")
        cases.forEach { savedCasesComboBox.addItem(it.name) }

        deleteButton.isEnabled = false
    }

    private fun loadSelectedCase() {
        val selectedName = savedCasesComboBox.selectedItem?.toString()
        if (selectedName == null || selectedName == "-- Select --") {
            deleteButton.isEnabled = false
            return
        }

        deleteButton.isEnabled = true

        val service = HtaccessProjectService.getInstance(project)
        val testCase = service.getTestCase(selectedName) ?: return

        // Load into UI
        urlField.text = testCase.url
        rulesTextArea.text = testCase.rules

        // Clear and reload server variables
        while (viewModel.serverVariables.rowCount > 0) {
            viewModel.serverVariables.removeRow(0)
        }
        testCase.serverVariables.forEach { (key, value) ->
            viewModel.serverVariables.addRow(arrayOf(key, value))
        }

        // Clear previous results
        viewModel.lastResult?.let {
            traceListModel.clear()
            resultUrlLabel.text = ""
            rawResponseArea.text = ""
        }
    }

    private fun saveCurrentCase() {
        val validation = viewModel.validate()
        if (validation is HtaccessViewModel.ValidationResult.Invalid) {
            Messages.showErrorDialog(
                project,
                "Cannot save: ${validation.errors.joinToString(", ")}",
                "Validation Error"
            )
            return
        }

        val name = Messages.showInputDialog(
            project,
            "Enter a name for this test case:",
            "Save Test Case",
            null,
            "",
            null
        )

        if (name.isNullOrBlank()) {
            return
        }

        val testCase = SavedTestCase(
            name = name,
            url = urlField.text,
            rules = rulesTextArea.text,
            serverVariables = viewModel.getServerVariablesMap().toMutableMap()
        )

        val service = HtaccessProjectService.getInstance(project)
        service.saveTestCase(testCase)
        refreshSavedCases()

        // Select the newly saved case
        savedCasesComboBox.selectedItem = name

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Htaccess Tester")
            .createNotification(
                "Test case saved",
                "Saved as \"$name\"",
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    private fun deleteSelectedCase() {
        val selectedName = savedCasesComboBox.selectedItem?.toString()
        if (selectedName == null || selectedName == "-- Select --") {
            return
        }

        val confirmed = Messages.showYesNoDialog(
            project,
            "Delete test case \"$selectedName\"?",
            "Confirm Delete",
            null
        )

        if (confirmed == Messages.YES) {
            val service = HtaccessProjectService.getInstance(project)
            service.deleteTestCase(selectedName)
            refreshSavedCases()
        }
    }

    private fun setupEditorListener() {
        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) {
                    updateEditorState()
                    if (useEditorCheckbox.isSelected) {
                        syncFromEditor()
                    }
                }
            }
        )
    }

    private fun updateEditorState() {
        val hasHtaccess = EditorUtils.hasActiveHtaccessFile(project)

        if (hasHtaccess) {
            val content = EditorUtils.getActiveHtaccessContent(project)
            editorFileLabel.text = content?.filePath ?: ""
            useEditorCheckbox.isEnabled = true
        } else {
            editorFileLabel.text = if (useEditorCheckbox.isSelected) "(no .htaccess file open)" else ""
            useEditorCheckbox.isEnabled = hasHtaccess || useEditorCheckbox.isSelected
        }

        // Disable text area editing when using editor
        rulesTextArea.isEditable = !useEditorCheckbox.isSelected
        rulesTextArea.background = if (useEditorCheckbox.isSelected) {
            JBColor.PanelBackground
        } else {
            JBColor.background()
        }
    }

    private fun syncFromEditor() {
        val content = EditorUtils.getActiveHtaccessContent(project)
        if (content != null) {
            rulesTextArea.text = content.content
            viewModel.rules = content.content
            editorFileLabel.text = content.filePath
        } else {
            editorFileLabel.text = "(no .htaccess file open)"
        }
    }

    /**
     * Loads rules from a specific file path. Used by the editor context action.
     */
    fun loadFromFile(filePath: String, content: String) {
        useEditorCheckbox.isSelected = true
        rulesTextArea.text = content
        viewModel.rules = content
        editorFileLabel.text = filePath
        updateEditorState()
    }

    override fun dispose() {
        viewModel.dispose()
    }

    /**
     * Custom cell renderer for the trace list showing rule evaluation results.
     */
    private class ResultLineCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): java.awt.Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

            if (value is ResultLine) {
                val statusIcon = when {
                    !value.isValid -> "\u274C" // Red X
                    value.isMet -> "\u2705"    // Green check
                    else -> "\u2796"           // Neutral dash
                }

                text = "$statusIcon ${value.line}"
                toolTipText = buildToolTip(value)

                if (!isSelected) {
                    foreground = when {
                        !value.isValid -> JBColor.RED
                        !value.wasReached -> JBColor.GRAY
                        value.isMet -> JBColor(0x228B22, 0x90EE90) // Forest green / light green
                        else -> JBColor.foreground()
                    }
                }
            }

            return this
        }

        private fun buildToolTip(line: ResultLine): String {
            return buildString {
                append("<html>")
                append("<b>Line:</b> ${line.line}<br>")
                line.message?.let { append("<b>Message:</b> $it<br>") }
                append("<b>Met:</b> ${line.isMet}<br>")
                append("<b>Valid:</b> ${line.isValid}<br>")
                append("<b>Reached:</b> ${line.wasReached}")
                append("</html>")
            }
        }
    }
}
