package controllers

import ui.NumericTextField
import java.awt.Dimension
import java.awt.GridLayout
import java.text.ParseException
import javax.swing.GroupLayout
import javax.swing.GroupLayout.Alignment
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFormattedTextField
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class GridRegionChooserController constructor(
    owner: JFrame,
    title: String,
    private var loadRegionsCallback: (IntArray) -> Unit
) : JDialog(owner, title) {
    private var gridLayout = GridLayout()
    private var gridPanel = JPanel(gridLayout)

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        preferredSize = Dimension(500, 500)

        val groups = GroupLayout(contentPane)
        layout = groups

        resizeGrid(2, 2)

        val gridWidthField = NumericTextField.create(gridLayout.columns, 2, MAX_WIDTH).apply {
            maximumSize = Dimension(maximumSize.width, preferredSize.height)
        }
        val gridHeightField = NumericTextField.create(gridLayout.rows, 2, MAX_HEIGHT).apply {
            maximumSize = Dimension(maximumSize.width, preferredSize.height)
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
                                .addComponent(gridWidthField, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(lblGridX)
                                .addComponent(lblGridHeight)
                                .addComponent(gridHeightField, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        )
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

    private fun resizeGrid(width: Int, height: Int) {
        if (width == gridLayout.columns && height == gridLayout.rows) return

        println("New size: $width x $height")

        gridPanel.removeAll()
        gridLayout = GridLayout(height, width)
        gridPanel.layout = gridLayout

        // add components
        for (x in 0 until width) {
            for (y in 0 until height) {
                gridPanel.add(
                    NumericTextField.create(0, 4647, 15522).apply {
                        maximumSize = Dimension(maximumSize.width, preferredSize.height)
                    }
                )
            }
        }

        pack()
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
