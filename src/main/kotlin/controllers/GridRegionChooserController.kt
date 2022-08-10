package controllers

import ui.NumericTextField
import java.awt.Dimension
import javax.swing.GroupLayout
import javax.swing.GroupLayout.Alignment
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel

class GridRegionChooserController constructor(
    owner: JFrame,
    title: String,
    private var loadRegionsCallback: (IntArray) -> Unit
) : JDialog(owner, title) {
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
                .addComponent(
                    lblGridSize
                )
                .addGroup(
                    groups.createParallelGroup(Alignment.CENTER)
                        .addComponent(lblGridWidth)
                        .addComponent(gridWidthField)
                        .addComponent(lblGridX)
                        .addComponent(lblGridHeight)
                        .addComponent(gridHeightField)
                )
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
                )
                .addGap(0, 0, Int.MAX_VALUE)
        )

        pack()

        rootPane.defaultButton = loadButton
        gridWidthField.requestFocus()
    }
}
