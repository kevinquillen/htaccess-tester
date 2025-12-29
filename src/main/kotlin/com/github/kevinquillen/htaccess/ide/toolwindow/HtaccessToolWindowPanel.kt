package com.github.kevinquillen.htaccess.ide.toolwindow

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.github.kevinquillen.htaccess.http.HtaccessApiException
import com.github.kevinquillen.htaccess.settings.HtaccessSettingsService
import com.intellij.ui.components.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.github.kevinquillen.htaccess.domain.model.ResultLine
import com.github.kevinquillen.htaccess.domain.model.TraceFilter
import com.github.kevinquillen.htaccess.ide.editor.EditorUtils
import com.github.kevinquillen.htaccess.settings.HtaccessProjectService
import com.github.kevinquillen.htaccess.settings.SavedTestCase
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.datatransfer.StringSelection
import javax.swing.*
import javax.swing.table.AbstractTableModel
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

    // Saved cases components
    private val savedCasesComboBox = JComboBox<String>()
    private val saveButton = JButton("Save")
    private val deleteButton = JButton("Delete")

    // Output components
    private val statsLabel = JBLabel("")
    private val traceFilterComboBox = JComboBox(TraceFilter.entries.map { it.displayName }.toTypedArray())
    private val copySummaryButton = JButton("Copy Summary")
    private val viewRawOutputButton = JButton("View Raw Output")
    private val traceTableModel = ResultLineTableModel()
    private val traceTable = JBTable(traceTableModel)
    private val progressBar = JProgressBar()
    private var currentFilter = TraceFilter.ALL
    private var lastRawResponse: String = ""

    init {
        buildUI()
        setupListeners()
        setupEditorListener()
        viewModel.onStateChanged = { refreshUI() }
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

        // Test button and filter controls on the left
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0))
        testButton.toolTipText = "Test the URL against the rules"
        leftPanel.add(testButton)
        leftPanel.add(JBLabel("Filter:"))
        traceFilterComboBox.toolTipText = "Filter trace results"
        leftPanel.add(traceFilterComboBox)
        copySummaryButton.toolTipText = "Copy test summary to clipboard"
        copySummaryButton.isEnabled = false
        leftPanel.add(copySummaryButton)
        viewRawOutputButton.toolTipText = "View the raw JSON response from the API"
        viewRawOutputButton.isEnabled = false
        leftPanel.add(viewRawOutputButton)
        panel.add(leftPanel, BorderLayout.WEST)

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
        val panel = JPanel(BorderLayout(0, 5))
        panel.border = JBUI.Borders.emptyTop(10)

        // Stats row
        statsLabel.foreground = JBColor.GRAY
        panel.add(statsLabel, BorderLayout.NORTH)

        // Trace table setup
        traceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        traceTable.rowHeight = 24
        traceTable.tableHeader.reorderingAllowed = false

        // Status column (icon) - fixed narrow width
        traceTable.columnModel.getColumn(0).preferredWidth = 30
        traceTable.columnModel.getColumn(0).maxWidth = 40
        traceTable.columnModel.getColumn(0).minWidth = 30
        traceTable.columnModel.getColumn(0).cellRenderer = StatusIconRenderer()

        // Rule column - takes remaining space
        traceTable.columnModel.getColumn(1).cellRenderer = RuleColumnRenderer()

        val traceScrollPane = JBScrollPane(traceTable)
        traceScrollPane.border = BorderFactory.createTitledBorder("Trace")
        traceScrollPane.preferredSize = Dimension(400, 200)
        panel.add(traceScrollPane, BorderLayout.CENTER)

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

        // Filter combo box
        traceFilterComboBox.addActionListener {
            currentFilter = TraceFilter.entries[traceFilterComboBox.selectedIndex]
            applyFilter()
        }

        // Copy summary button
        copySummaryButton.addActionListener { copySummary() }

        // View raw output button
        viewRawOutputButton.addActionListener { showRawOutputDialog() }

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

        // Show first-run notice if not yet acknowledged
        if (!checkFirstRunAcknowledged()) {
            return
        }

        viewModel.runTest { _, error ->
            if (error != null) {
                showErrorNotification(error)
            }
        }
    }

    private fun checkFirstRunAcknowledged(): Boolean {
        val settings = HtaccessSettingsService.getInstance()
        if (settings.firstRunAcknowledged) {
            return true
        }

        val result = FirstRunNoticeDialog(project).showAndGet()
        if (result) {
            settings.firstRunAcknowledged = true
        }
        return result
    }

    private fun showErrorNotification(error: String) {
        // Try to get more context from the last exception if available
        val exception = viewModel.lastException

        val (title, message) = when {
            exception is HtaccessApiException && exception.isRateLimited -> {
                "Rate Limit Exceeded" to "The htaccess testing service has rate limited your request. Please wait a moment before testing again."
            }
            exception is HtaccessApiException && exception.isSchemaError -> {
                "API Response Changed" to "The API response format has changed unexpectedly. Please check for plugin updates or try again later."
            }
            exception is HtaccessApiException && exception.statusCode in 500..599 -> {
                "Service Unavailable" to "The htaccess testing service is temporarily unavailable. Please try again in a few moments."
            }
            exception is HtaccessApiException && exception.message?.contains("timed out", ignoreCase = true) == true -> {
                "Request Timeout" to "The request took too long to complete. Check your network connection or try again."
            }
            else -> {
                "Test Failed" to error
            }
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Htaccess Tester")
            .createNotification(title, message, NotificationType.ERROR)
            .notify(project)
    }

    private fun refreshUI() {
        // Update progress bar and button
        progressBar.isVisible = viewModel.isLoading
        testButton.isEnabled = !viewModel.isLoading

        // Update results
        val result = viewModel.lastResult
        if (result != null) {
            // Update stats
            val stats = TraceFilter.calculateStats(result.lines)
            statsLabel.text = "${stats.total} rules: ${stats.met} met, ${stats.notMet} not met, ${stats.invalid} invalid"

            // Apply filter and populate trace list
            applyFilter()

            lastRawResponse = result.rawResponse
            copySummaryButton.isEnabled = true
            viewRawOutputButton.isEnabled = true
        } else {
            statsLabel.text = ""
            traceTableModel.clear()
            lastRawResponse = viewModel.lastError ?: ""
            copySummaryButton.isEnabled = false
            viewRawOutputButton.isEnabled = lastRawResponse.isNotEmpty()
        }
    }

    private fun applyFilter() {
        val result = viewModel.lastResult ?: return
        val filteredLines = TraceFilter.filter(result.lines, currentFilter)
        traceTableModel.setLines(filteredLines)
    }

    private fun copySummary() {
        val result = viewModel.lastResult ?: return
        val summary = TraceFilter.generateSummary(result)

        CopyPasteManager.getInstance().setContents(StringSelection(summary))

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Htaccess Tester")
            .createNotification(
                "Summary copied to clipboard",
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    private fun showRawOutputDialog() {
        if (lastRawResponse.isEmpty()) return
        RawOutputDialog(project, lastRawResponse).show()
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
            traceTableModel.clear()
            lastRawResponse = ""
            viewRawOutputButton.isEnabled = false
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
     * Table model for displaying ResultLine data in the trace table.
     */
    private class ResultLineTableModel : AbstractTableModel() {
        private val lines = mutableListOf<ResultLine>()
        private val columnNames = arrayOf("", "Rule / Response")

        fun setLines(newLines: List<ResultLine>) {
            lines.clear()
            lines.addAll(newLines)
            fireTableDataChanged()
        }

        fun clear() {
            lines.clear()
            fireTableDataChanged()
        }

        fun getLineAt(row: Int): ResultLine? = lines.getOrNull(row)

        override fun getRowCount(): Int = lines.size
        override fun getColumnCount(): Int = 2
        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
            val line = lines.getOrNull(rowIndex) ?: return null
            return when (columnIndex) {
                0 -> line  // Status column - pass the whole line for icon rendering
                1 -> line  // Rule column - pass the whole line for text rendering
                else -> null
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
    }

    /**
     * Renders the status icon column in the trace table.
     * - Green check: met and valid
     * - Red X: invalid
     * - Yellow warning: not supported or not reached
     * - Red X: not met but valid and reached
     */
    private class StatusIconRenderer : DefaultTableCellRenderer() {
        init {
            horizontalAlignment = CENTER
        }

        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): java.awt.Component {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

            if (value is ResultLine) {
                text = null
                icon = when {
                    !value.isValid -> AllIcons.General.Error
                    !value.isSupported -> AllIcons.General.Warning
                    !value.wasReached -> AllIcons.General.Warning
                    value.isMet -> AllIcons.General.InspectionsOK
                    else -> AllIcons.General.Error
                }
            }

            return this
        }
    }

    /**
     * Renders the rule/response column in the trace table.
     */
    private class RuleColumnRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): java.awt.Component {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

            if (value is ResultLine) {
                // Show the rule line, and message if available
                text = if (value.message != null) {
                    "${value.line} — ${value.message}"
                } else {
                    value.line
                }

                toolTipText = buildToolTip(value)

                if (!isSelected) {
                    foreground = when {
                        !value.isValid -> JBColor.RED
                        !value.isSupported -> JBColor(0xB8860B, 0xDAA520)
                        !value.wasReached -> JBColor.GRAY
                        value.isMet -> JBColor(0x228B22, 0x90EE90)
                        else -> JBColor.RED                              // Red - not met
                    }
                }
            }

            return this
        }

        private fun buildToolTip(line: ResultLine): String {
            return buildString {
                append("<html>")
                append("<b>Rule:</b> ${line.line}<br>")
                line.message?.let { append("<b>Message:</b> $it<br>") }
                append("<b>Met:</b> ${line.isMet}<br>")
                append("<b>Valid:</b> ${line.isValid}<br>")
                append("<b>Reached:</b> ${line.wasReached}<br>")
                append("<b>Supported:</b> ${line.isSupported}")
                append("</html>")
            }
        }
    }
}

/**
 * Dialog shown on first run to explain remote evaluation.
 */
private class FirstRunNoticeDialog(project: Project) : DialogWrapper(project, true) {

    init {
        title = "Htaccess Tester - Remote Evaluation Notice"
        setOKButtonText("I Understand, Continue")
        setCancelButtonText("Cancel")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, 10))
        panel.preferredSize = Dimension(500, 280)
        panel.border = JBUI.Borders.empty(10)

        val messageArea = JBTextArea()
        messageArea.isEditable = false
        messageArea.lineWrap = true
        messageArea.wrapStyleWord = true
        messageArea.background = panel.background
        messageArea.text = buildString {
            appendLine("This plugin uses a remote service to evaluate .htaccess rules.")
            appendLine()
            appendLine("When you click 'Test', the following data is sent to htaccess.madewithlove.com:")
            appendLine()
            appendLine("  - The URL you want to test")
            appendLine("  - Your .htaccess rules")
            appendLine("  - Any server variables you've configured")
            appendLine()
            appendLine("This service is provided by madewithlove and is used by many developers to test Apache rewrite rules.")
            appendLine()
            appendLine("Important notes:")
            appendLine("  - Do not include sensitive information in your rules or URLs")
            appendLine("  - The service may log requests for debugging purposes")
            appendLine("  - Requires an active internet connection")
            appendLine("-----")
            appendLine("Author: Kevin Quillen")
            appendLine("Repository: https://github.com/kevinquillen/htaccess-tester")
        }

        panel.add(JBScrollPane(messageArea), BorderLayout.CENTER)

        return panel
    }
}

/**
 * Dialog to display raw JSON response from the API.
 */
private class RawOutputDialog(
    project: Project,
    private val rawOutput: String
) : DialogWrapper(project, true) {

    init {
        title = "Raw API Response"
        setOKButtonText("Close")
        setCancelButtonText("Copy to Clipboard")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, 10))
        panel.preferredSize = Dimension(600, 400)
        panel.border = JBUI.Borders.empty(10)

        val textArea = JBTextArea()
        textArea.isEditable = false
        textArea.text = formatJson(rawOutput)
        textArea.caretPosition = 0

        val scrollPane = JBScrollPane(textArea)
        panel.add(scrollPane, BorderLayout.CENTER)

        return panel
    }

    override fun doCancelAction() {
        CopyPasteManager.getInstance().setContents(StringSelection(rawOutput))
        super.doCancelAction()
    }

    private fun formatJson(json: String): String {
        return try {
            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            val element = com.google.gson.JsonParser.parseString(json)
            gson.toJson(element)
        } catch (e: Exception) {
            json
        }
    }
}
