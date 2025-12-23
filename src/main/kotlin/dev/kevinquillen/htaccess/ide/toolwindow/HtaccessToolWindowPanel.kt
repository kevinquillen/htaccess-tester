package dev.kevinquillen.htaccess.ide.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.kevinquillen.htaccess.domain.model.ResultLine
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer

class HtaccessToolWindowPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {

    private val viewModel = HtaccessViewModel()

    // Input components
    private val urlField = JBTextField()
    private val rulesTextArea = JBTextArea(10, 40)
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

    // Output components
    private val resultUrlLabel = JBLabel("")
    private val traceListModel = DefaultListModel<ResultLine>()
    private val traceList = JBList(traceListModel)
    private val rawResponseArea = JBTextArea(5, 40)
    private val progressBar = JProgressBar()

    init {
        buildUI()
        setupListeners()
        viewModel.onStateChanged = { refreshUI() }

        // Share is disabled until Stage 4
        shareButton.isEnabled = false
        shareButton.toolTipText = "Coming in a future update"
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

        // Rules TextArea
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.fill = GridBagConstraints.NONE
        gbc.weightx = 0.0
        gbc.anchor = GridBagConstraints.NORTHWEST
        mainPanel.add(JBLabel("Rules:"), gbc)

        gbc.gridx = 1
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
        gbc.gridy = 2
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
        gbc.gridy = 3
        gbc.gridwidth = 2
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.insets = JBUI.insets(10, 0, 10, 0)
        mainPanel.add(buildButtonsPanel(), gbc)

        // Progress Bar
        gbc.gridy = 4
        progressBar.isIndeterminate = true
        progressBar.isVisible = false
        mainPanel.add(progressBar, gbc)

        // Output Section
        gbc.gridy = 5
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

    private fun buildButtonsPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0))
        testButton.toolTipText = "Test the URL against the rules"
        shareButton.toolTipText = "Share this test case (coming soon)"
        panel.add(testButton)
        panel.add(shareButton)
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

        // Test button
        testButton.addActionListener { runTest() }
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

    private fun refreshUI() {
        // Update progress bar
        progressBar.isVisible = viewModel.isLoading
        testButton.isEnabled = !viewModel.isLoading

        // Update results
        val result = viewModel.lastResult
        if (result != null) {
            resultUrlLabel.text = result.outputUrl ?: "(no output URL)"

            traceListModel.clear()
            result.lines.forEach { traceListModel.addElement(it) }

            rawResponseArea.text = result.rawResponse
        } else {
            resultUrlLabel.text = ""
            traceListModel.clear()
            rawResponseArea.text = viewModel.lastError ?: ""
        }
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
