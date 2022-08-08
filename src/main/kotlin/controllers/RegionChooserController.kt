package controllers

import ui.JLinkLabel
import ui.NumericTextField
import ui.PlaceholderTextField
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.CENTER
import java.awt.GridBagConstraints.HORIZONTAL
import java.awt.GridBagConstraints.LINE_START
import java.awt.GridBagConstraints.NONE
import java.awt.GridBagConstraints.PAGE_END
import java.awt.GridBagConstraints.PAGE_START
import java.awt.GridBagConstraints.REMAINDER
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel

class RegionChooserController constructor(
    owner: JFrame,
    title: String,
    private var loadRegionCallback: (Int, Int) -> Unit
) : JDialog(owner, title) {
    private val errorMessageLabel: JLabel

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        preferredSize = Dimension(500, 250)
        layout = GridBagLayout()

        val regionIdField = PlaceholderTextField("", "10038").apply {
            maximumSize = Dimension(maximumSize.width, preferredSize.height)
        }
        val radiusField = NumericTextField.create(1, 1, 20).apply {
            maximumSize = Dimension(maximumSize.width, preferredSize.height)
        }
        errorMessageLabel = JLabel().apply {
            foreground = Color.RED
        }

        val lblExplv = JLabel("Visit Explv's OSRS map to find region ids.")
        val lnkExplv = JLinkLabel("https://explv.github.io/")
        val lblWarn = JLabel("Note: Higher radius will affect load time and FPS.")
        val lblRegion = JLabel("Region ID:").apply {
            displayedMnemonic = 'R'.code
            labelFor = regionIdField
        }
        val lblRadius = JLabel("Radius:").apply {
            displayedMnemonic = 'a'.code
            labelFor = radiusField
        }

        val loadButton = JButton("Load Region").apply {
            alignmentX = CENTER_ALIGNMENT
            mnemonic = 'L'.code
            addActionListener {
                loadRegion(regionIdField.text, radiusField.value as Int)
            }
        }

        val pnlExplv = JPanel().apply {
            layout = FlowLayout()
            add(lblExplv)
            add(lnkExplv)
        }

        val inset = Insets(4, 4, 4, 4)
        add(
            pnlExplv,
            GridBagConstraints(0, 0, REMAINDER, 1, 1.0, 1.0, PAGE_END, NONE, inset, 0, 0)
        )
        add(
            lblWarn,
            GridBagConstraints(0, 1, REMAINDER, 1, 1.0, 0.0, CENTER, NONE, inset, 0, 0)
        )
        add(
            lblRegion,
            GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, LINE_START, NONE, inset, 0, 0)
        )
        add(
            lblRadius,
            GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, LINE_START, NONE, inset, 0, 0)
        )
        add(
            regionIdField,
            GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL, inset, 0, 0)
        )
        add(
            radiusField,
            GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL, inset, 0, 0)
        )
        add(
            loadButton,
            GridBagConstraints(0, 4, REMAINDER, 1, 0.0, 0.0, CENTER, NONE, inset, 0, 0)
        )
        add(
            errorMessageLabel,
            GridBagConstraints(0, 5, REMAINDER, 1, 1.0, 1.0, PAGE_START, NONE, inset, 0, 0)
        )

        rootPane.defaultButton = loadButton
        pack()

        regionIdField.requestFocus()
    }

    private fun loadRegion(regionIdStr: String, radius: Int) {
        errorMessageLabel.text = ""

        val regionId: Int? = regionIdStr.toIntOrNull()
        if (regionId == null || (regionId < 4647 || regionId > 15522)) {
            errorMessageLabel.text = INVALID_REGION_ID_TEXT
            return
        }

        dispose()
        loadRegionCallback(regionId, radius)
    }

    companion object {
        private const val INVALID_REGION_ID_TEXT = "Region Id must be between 4647 and 15522."
    }
}
