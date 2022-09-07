package controllers

import com.fasterxml.jackson.databind.ObjectMapper
import controllers.worldRenderer.Constants.MAP_LENGTH
import models.locations.Location
import models.locations.Locations
import org.slf4j.LoggerFactory
import ui.FilteredListModel
import ui.JLinkLabel
import ui.NumericTextField
import ui.PlaceholderTextField
import ui.listener.FilterTextListener
import utils.Utils
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.ABOVE_BASELINE
import java.awt.GridBagConstraints.BOTH
import java.awt.GridBagConstraints.CENTER
import java.awt.GridBagConstraints.HORIZONTAL
import java.awt.GridBagConstraints.LINE_END
import java.awt.GridBagConstraints.NONE
import java.awt.GridBagConstraints.PAGE_END
import java.awt.GridBagConstraints.PAGE_START
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFormattedTextField
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.UIManager

class LocationSearchController(
    owner: JFrame,
    title: String,
    private val loadRegionCallback: (Component, Int, Int) -> Boolean,
) : JDialog(owner, title) {
    private val logger = LoggerFactory.getLogger(LocationSearchController::class.java)
    private val listLocations: JList<Location>
    private val txtSearchQuery: JTextField
    private val txtRadius: JFormattedTextField
    private val errorMessageLabel: JLabel

    init {
        layout = GridBagLayout()
        preferredSize = Dimension(600, 400)

        val filterableLocations = FilteredListModel(::locationToString)
        listLocations = JList(filterableLocations)
        val locationsScrollPane = JScrollPane(listLocations).apply {
            val borderColor = UIManager.getColor("InternalFrame.borderColor")
            border = BorderFactory.createMatteBorder(0, 1, 0, 0, borderColor)
            // Hack to prevent the width jumping when the horizontal scrollbar appears
            preferredSize = Dimension(1, 1)
        }
        listLocations.cellRenderer = LocationCell()
        txtSearchQuery = PlaceholderTextField("", "Lumbridge")
        txtRadius = NumericTextField.create(1, 1, MAP_LENGTH)
        val lblSearchQuery = JLabel("Search Query or Region ID:").apply {
            displayedMnemonic = 'S'.code
            labelFor = txtSearchQuery
        }
        val lblRadius = JLabel("Radius:").apply {
            displayedMnemonic = 'A'.code
            labelFor = txtRadius
        }
        val lblErrorText = JLabel("")
        lblErrorText.foreground = Color.RED
        val btnLoad = JButton("Load Location").apply {
            mnemonic = 'L'.code
        }
        errorMessageLabel = JLabel().apply {
            foreground = Color.RED
        }
        val pnlExplv = JPanel().apply {
            layout = FlowLayout()
            add(JLabel("Visit Explv's OSRS map to find more region ids."))
            add(JLinkLabel("https://explv.github.io/"))
        }

        val inset = Insets(0, 20, 0, 0)
        val controlSepInsets = Insets(30, 20, 0, 0)
        add(
            pnlExplv,
            GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, CENTER, NONE, inset, 0, 0)
        )
        add(
            locationsScrollPane,
            GridBagConstraints(1, 1, 1, 7, 5.0, 1.0, LINE_END, BOTH, inset, 0, 0)
        )
        add(
            lblSearchQuery,
            GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, PAGE_END, NONE, inset, 0, 0)
        )
        add(
            txtSearchQuery,
            GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL, inset, 0, 0)
        )
        add(
            lblRadius,
            GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, ABOVE_BASELINE, NONE, controlSepInsets, 0, 0)
        )
        add(
            txtRadius,
            GridBagConstraints(0, 4, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL, inset, 0, 0)
        )
        add(
            lblErrorText,
            GridBagConstraints(0, 5, 1, 1, 1.0, 0.0, CENTER, NONE, inset, 0, 0)
        )
        add(
            btnLoad,
            GridBagConstraints(0, 6, 1, 1, 1.0, 0.0, CENTER, NONE, controlSepInsets, 0, 0)
        )
        add(
            errorMessageLabel,
            GridBagConstraints(0, 7, 1, 1, 1.0, 1.0, PAGE_START, NONE, controlSepInsets, 0, 0)
        )

        // load locations
        filterableLocations.backingList = readBuiltinLocations()

        txtSearchQuery.document.addDocumentListener(FilterTextListener(txtSearchQuery, filterableLocations))

        // list view selection handler
        listLocations.apply {
            addListSelectionListener {
                if (selectedIndex != -1) {
                    btnLoad.isEnabled = true
                }
            }
        }

        // load button handler
        btnLoad.addActionListener { loadSelectedRegions() }

        rootPane.defaultButton = btnLoad
        pack()

        txtSearchQuery.requestFocus()
    }

    private fun loadSelectedRegions() {
        val selectedLocation = listLocations.selectedValue
        val enteredRegionId = txtSearchQuery.text.toIntOrNull()
        if (selectedLocation == null && enteredRegionId == null) {
            errorMessageLabel.text = "<html><center>Please select a location<br>or enter a region ID</center></html>"
            return
        }
        val regionId =
            if (selectedLocation != null) regionIdForLocation(selectedLocation)
            else enteredRegionId!!

        if (loadRegionCallback(this, regionId, txtRadius.value as Int)) {
            dispose()
        }
    }

    private fun readBuiltinLocations(): ArrayList<Location> {
        val locationMapper = ObjectMapper()

        val locations =
            locationMapper.readValue(
                this::class.java.getResource("/data/locations.json"),
                Locations::class.java
            )

        // Warn if some locations are duplicate.
        val duplicateLocations = locations.locations.groupBy(::regionIdForLocation).filter { it.value.size > 1 }
        if (duplicateLocations.isNotEmpty()) {
            val duplicateLocationsString = duplicateLocations.map {
                it.value.joinToString(
                    ", ",
                    prefix = "[",
                    postfix = "]",
                    transform = ::locationToString
                )
            }.joinToString("\n")
            logger.warn("Duplicate locations found:\n$duplicateLocationsString")
        }

        return locations.locations
    }

    private class LocationCell : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            item: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            super.getListCellRendererComponent(list, item, index, isSelected, cellHasFocus)
            border = BorderFactory.createEmptyBorder(0, 8, 0, 0)
            text =
                if (item == null) ""
                else locationToString(item as Location)
            return this
        }
    }

    companion object {
        fun regionIdForLocation(location: Location): Int {
            val (x, y) = location.coords
            return Utils.worldCoordinatesToRegionId(x, y)
        }

        private fun locationToString(location: Location): String {
            val regionId = regionIdForLocation(location)
            return "${location.name} [$regionId]"
        }
    }
}
