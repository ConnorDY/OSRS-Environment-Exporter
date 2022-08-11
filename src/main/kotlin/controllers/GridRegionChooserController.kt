package controllers

import ui.NumericTextField
import java.awt.Dimension
import java.awt.GridLayout
import java.text.ParseException
import javax.swing.GroupLayout
import javax.swing.GroupLayout.Alignment
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JFormattedTextField
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class GridRegionChooserController constructor(
    owner: JFrame,
    title: String,
    private var loadRegionsCallback: (IntArray) -> Unit
) : JDialog(owner, title) {
    private var gridLayout = GridLayout()
    private var gridPanel = JPanel(gridLayout)

    private var gridInputs: Array<Array<JFormattedTextField>> = emptyArray()

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        preferredSize = Dimension(470, 520)

        val groups = GroupLayout(contentPane)
        layout = groups

        resizeGrid(2, 2)

        val gridWidthField = NumericTextField.create(gridLayout.columns, 2, MAX_WIDTH).apply {
            sizeToText("888")
        }
        val gridHeightField = NumericTextField.create(gridLayout.rows, 2, MAX_HEIGHT).apply {
            sizeToText("888")
        }

        val sizeChangeListener = DocumentTextListener {
            val width = gridWidthField.calcValueOrNull() as Int?
            val height = gridHeightField.calcValueOrNull() as Int?
            if (width != null && height != null) resizeGrid(width, height)
        }

        arrayOf(gridWidthField.document, gridHeightField.document).forEach { it -> it.addDocumentListener(sizeChangeListener) }

        val lblGridSize = JLabel("Size").apply {}

        val lblGridWidth = JLabel("W:").apply {
            displayedMnemonic = 'w'.code
            labelFor = gridWidthField
        }

        val lblGridX = JLabel("x").apply {}

        val lblGridHeight = JLabel("H:").apply {
            displayedMnemonic = 'h'.code
            labelFor = gridHeightField
        }

        val chkBoxAutoPopulate = JCheckBox("Auto-populate", true)

        val loadButton = JButton("Load Grid Regions").apply {
            alignmentX = CENTER_ALIGNMENT
            mnemonic = 'L'.code
            addActionListener {
                // TODO: load the region grid
            }
        }

        groups.autoCreateGaps = true
        groups.autoCreateContainerGaps = true

        groups.setVerticalGroup(
            groups.createSequentialGroup()
                .addComponent(lblGridSize)
                .addGroup(
                    groups.createParallelGroup(Alignment.CENTER)
                        .addComponent(lblGridWidth)
                        .addComponent(gridWidthField)
                        .addComponent(lblGridX)
                        .addComponent(lblGridHeight)
                        .addComponent(gridHeightField)
                )
                .addComponent(chkBoxAutoPopulate)
                .addGap(0, 0, Int.MAX_VALUE)
                .addComponent(gridPanel)
                .addGap(0, 0, Int.MAX_VALUE)
                .addComponent(loadButton)

        )

        groups.setHorizontalGroup(
            groups.createSequentialGroup()
                .addGap(0, 0, Int.MAX_VALUE)
                .addGroup(
                    groups.createParallelGroup(Alignment.CENTER)
                        .addComponent(
                            lblGridSize
                        )
                        .addGroup(
                            groups.createSequentialGroup()
                                .addComponent(lblGridWidth)
                                .addComponent(gridWidthField)
                                .addComponent(lblGridX)
                                .addComponent(lblGridHeight)
                                .addComponent(gridHeightField)
                        )
                        .addComponent(chkBoxAutoPopulate)
                        .addComponent(gridPanel)
                        .addComponent(loadButton)
                )
                .addGap(0, 0, Int.MAX_VALUE)
        )

        pack()

        rootPane.defaultButton = loadButton
        gridWidthField.requestFocus()
    }

    private fun JFormattedTextField.calcValueOrNull(): Any? =
        try {
            formatter.stringToValue(text)
        } catch (e: ParseException) {
            null
        }

    private fun JTextField.sizeToText(sizeText: String) {
        val oldText = text
        text = sizeText
        maximumSize = preferredSize
        text = oldText
    }

    private fun resizeGrid(width: Int, height: Int) {
        if (width == gridLayout.columns && height == gridLayout.rows) return

        println("New size: $width x $height")

        gridPanel.removeAll()
        gridLayout = GridLayout(height, width)
        gridPanel.layout = gridLayout
        gridLayout.hgap = 10
        gridLayout.vgap = 10

        gridInputs = Array(width) { x ->
            Array(height) { y ->
                val input = NumericTextField.createNullable(null, 4647, 15522).apply {
                    minimumSize = Dimension(36, 36)
                    maximumSize = minimumSize
                }

                val changeListener = DocumentTextListener {
                    autoPopulate(x, y, input.calcValueOrNull() as Int?)
                }

                input.document.addDocumentListener(changeListener)

                gridPanel.add(input)

                input
            }
        }

        pack()
    }

    private fun autoPopulate(x: Int, y: Int, regionId: Int?) {
        println("$x, $y: $regionId")
    }

    private class DocumentTextListener(private val onChange: (event: DocumentEvent) -> Unit) : DocumentListener {
        override fun insertUpdate(event: DocumentEvent?) {
            event?.let(onChange)
        }

        override fun removeUpdate(event: DocumentEvent?) {
            event?.let(onChange)
        }

        override fun changedUpdate(event: DocumentEvent?) {}
    }

    companion object {
        const val MAX_WIDTH = 6
        const val MAX_HEIGHT = 6
    }
}
