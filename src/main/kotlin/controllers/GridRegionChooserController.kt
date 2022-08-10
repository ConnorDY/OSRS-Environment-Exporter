package controllers

import ui.NumericTextField
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.GroupLayout
import javax.swing.GroupLayout.Alignment
import javax.swing.JButton
import javax.swing.JDialog
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
    private var gridPanel = JPanel(GridLayout())

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        preferredSize = Dimension(300, 500)

        val groups = GroupLayout(contentPane)
        layout = groups

        val gridWidthField = NumericTextField.create(2, 1, 5).apply {
            maximumSize = Dimension(maximumSize.width, preferredSize.height)
        }
        val gridHeightField = NumericTextField.create(2, 1, 5).apply {
            maximumSize = Dimension(maximumSize.width, preferredSize.height)
        }

        val sizeChangeListener = DocumentTextListener {
            val width = gridWidthField.value as Int
            val height = gridHeightField.value as Int
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

    private fun resizeGrid(width: Int, height: Int) {
        println("Resizing")

        gridPanel.removeAll()
        gridPanel.layout = GridLayout(width, height)

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
}
