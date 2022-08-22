package controllers

import AppConstants
import controllers.RegionLoadingDialogHelper.MAP_LENGTH
import controllers.RegionLoadingDialogHelper.confirmRegionLoad
import ui.NumericTextField
import ui.listener.DocumentTextListener
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.CENTER
import java.awt.GridBagConstraints.NONE
import java.awt.GridBagLayout
import java.awt.Insets
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
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.LayoutStyle
import kotlin.math.max

class GridRegionChooserController(
    owner: JFrame,
    title: String,
    private var loadRegionsCallback: (List<List<Int?>>) -> Unit
) : JDialog(owner, title) {
    private var gridPanel = JPanel(GridBagLayout())

    private var gridInputs: Array<Array<JFormattedTextField>> = emptyArray()
    private var autoPopulating = false
    private val chkBoxAutoPopulate: JCheckBox

    private val rows get() = gridInputs.size
    private val cols get() = if (gridInputs.isEmpty()) 0 else gridInputs[0].size

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        preferredSize = Dimension(500, 550)

        val groups = GroupLayout(contentPane)
        layout = groups

        resizeGrid(2, 2)

        val gridWidthField = NumericTextField.create(cols, 1, MAP_LENGTH).apply {
            sizeToText("888")
        }
        val gridHeightField = NumericTextField.create(rows, 1, MAP_LENGTH).apply {
            sizeToText("888")
        }

        val sizeChangeListener = DocumentTextListener {
            val width = gridWidthField.calcValueOrNull() as Int?
            val height = gridHeightField.calcValueOrNull() as Int?
            if (width != null && height != null) resizeGrid(width, height)
        }

        arrayOf(gridWidthField.document, gridHeightField.document).forEach { it -> it.addDocumentListener(sizeChangeListener) }

        val lblGridDimensions = JLabel("Grid Dimensions").apply {}

        val lblGridWidth = JLabel("W:").apply {
            displayedMnemonic = 'W'.code
            labelFor = gridWidthField
        }

        val lblGridX = JLabel("x").apply {}

        val lblGridHeight = JLabel("H:").apply {
            displayedMnemonic = 'H'.code
            labelFor = gridHeightField
        }

        chkBoxAutoPopulate = JCheckBox("Auto-populate", true).apply {
            toolTipText = "Auto-fill grid with adjacent regions when a cell is edited"
            mnemonic = 'P'.code
        }

        val lblInstructions = JLabel("Input region ID(s) into the boxes below.").apply {}

        val gridScrollPane = JScrollPane(gridPanel)

        val loadButton = JButton("Load Grid Regions").apply {
            alignmentX = CENTER_ALIGNMENT
            mnemonic = 'L'.code
            addActionListener {
                val numRegions = gridInputs.sumOf { row -> row.count { it.value != null } }
                if (confirmRegionLoad(this@GridRegionChooserController, numRegions)) {
                    loadRegionsCallback(
                        gridInputs.map { row ->
                            row.map {
                                it.value as Int?
                            }
                        }.reversed()
                    )
                }
                dispose()
            }
        }

        groups.autoCreateGaps = true
        groups.autoCreateContainerGaps = true

        groups.setVerticalGroup(
            groups.createSequentialGroup()
                .addComponent(lblGridDimensions)
                .addGroup(
                    groups.createParallelGroup(Alignment.BASELINE)
                        .addComponent(lblGridWidth)
                        .addComponent(gridWidthField)
                        .addComponent(lblGridX)
                        .addComponent(lblGridHeight)
                        .addComponent(gridHeightField)
                )
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chkBoxAutoPopulate)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Int.MAX_VALUE)
                .addComponent(lblInstructions)
                .addComponent(gridScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Int.MAX_VALUE)
                .addComponent(loadButton)

        )

        groups.setHorizontalGroup(
            groups.createSequentialGroup()
                .addGap(0, 0, Int.MAX_VALUE)
                .addGroup(
                    groups.createParallelGroup(Alignment.CENTER)
                        .addComponent(
                            lblGridDimensions
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
                        .addComponent(lblInstructions)
                        .addComponent(gridScrollPane)
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
        if (height == rows && width == cols) return

        gridPanel.removeAll()

        gridInputs = Array(height) { y ->
            Array(width) { x ->
                val input = NumericTextField.createNullable(null, AppConstants.REGION_ID_MIN, AppConstants.REGION_ID_MAX).apply {
                    sizeToText("888888")
                    val maxDim = max(maximumSize.width, maximumSize.height)
                    val size = Dimension(maxDim, maxDim)
                    minimumSize = size
                    preferredSize = size
                    maximumSize = size

                    horizontalAlignment = JTextField.CENTER
                }

                input.document.addDocumentListener(
                    DocumentTextListener {
                        autoPopulate(x, y, input.calcValueOrNull() as Int?)
                    }
                )

                val constraints =
                    GridBagConstraints(x, y, 1, 1, 0.0, 0.0, CENTER, NONE, Insets(5, 5, 5, 5), 0, 0)
                gridPanel.add(input, constraints)

                input
            }
        }

        pack()
    }

    private fun autoPopulate(x: Int, y: Int, regionId: Int?) {
        if (autoPopulating || regionId == null || !chkBoxAutoPopulate.isSelected) return
        autoPopulating = true

        for ((yy, inputs) in gridInputs.withIndex()) {
            for ((xx, input) in inputs.withIndex()) {
                if (xx == x && yy == y) continue

                val xdiff = xx - x
                val ydiff = yy - y

                input.value = regionId + (xdiff * 256) - ydiff
            }
        }

        autoPopulating = false
    }
}
