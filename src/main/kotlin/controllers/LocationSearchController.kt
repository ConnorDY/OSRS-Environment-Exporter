package controllers

import com.fasterxml.jackson.databind.ObjectMapper
import models.locations.Location
import models.locations.Locations
import models.scene.Scene
import ui.FilteredListModel
import ui.PlaceholderTextField
import ui.listener.FilterTextListener
import utils.Utils
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
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
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.UIManager

class LocationSearchController constructor(
    owner: JFrame,
    title: String,
    private val scene: Scene
) : JDialog(owner, title) {

    init {
        layout = GridBagLayout()
        preferredSize = Dimension(600, 400)

        val filterableLocations = FilteredListModel<Location> { it.name }
        val listLocations = JList(filterableLocations).apply {
            val borderColor = UIManager.getColor("InternalFrame.borderColor")
            border = BorderFactory.createMatteBorder(0, 1, 0, 0, borderColor)
        }
        listLocations.cellRenderer = LocationCell()
        val txtSearchQuery = PlaceholderTextField("", "Lumbridge")
        val txtRadius = JTextField("1")
        val lblErrorText = JLabel("")
        lblErrorText.foreground = Color.RED
        val btnLoad = JButton("Load Location")

        val inset = Insets(0, 20, 0, 0)
        val controlSepInsets = Insets(30, 20, 0, 0)
        add(
            JScrollPane(listLocations),
            GridBagConstraints(1, 0, 1, 6, 1.5, 1.0, LINE_END, BOTH, inset, 0, 0)
        )
        add(
            JLabel("Search Query:"),
            GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, PAGE_END, NONE, inset, 0, 0)
        )
        add(
            txtSearchQuery,
            GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL, inset, 0, 0)
        )
        add(
            JLabel("Radius:"),
            GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, ABOVE_BASELINE, NONE, controlSepInsets, 0, 0)
        )
        add(
            txtRadius,
            GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, CENTER, HORIZONTAL, inset, 0, 0)
        )
        add(
            lblErrorText,
            GridBagConstraints(0, 4, 1, 1, 1.0, 0.0, CENTER, NONE, inset, 0, 0)
        )
        add(
            btnLoad,
            GridBagConstraints(0, 5, 1, 1, 1.0, 1.0, PAGE_START, NONE, controlSepInsets, 0, 0)
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
        btnLoad.addActionListener {
            val selectedLocation = listLocations.selectedValue ?: return@addActionListener
            val regionId = regionIdForLocation(selectedLocation)

            val radius: Int? = txtRadius.text.toIntOrNull()
            if (radius == null || (radius < 1 || radius > 20)) {
                lblErrorText.text = RegionChooserController.INVALID_RADIUS_TEXT
                lblErrorText.isVisible = true
                return@addActionListener
            }
            dispose()
            scene.load(regionId, radius)
        }

        pack()
    }

    private fun readBuiltinLocations(): ArrayList<Location> {
        val locationMapper = ObjectMapper()

        val locations =
            locationMapper.readValue(
                this::class.java.getResource("/data/locations.json"),
                Locations::class.java
            )

        val locations1 = locations.locations
        return locations1
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
                else {
                    val regionId = regionIdForLocation(item as Location)
                    "${item.name} ($regionId)"
                }
            return this
        }
    }

    companion object {
        fun regionIdForLocation(location: Location): Int {
            val (x, y) = location.coords
            return Utils.worldCoordinatesToRegionId(x, y)
        }
    }
}
