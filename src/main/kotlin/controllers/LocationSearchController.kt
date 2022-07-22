package controllers

import com.fasterxml.jackson.databind.ObjectMapper
import models.locations.Location
import models.locations.Locations
import models.scene.Scene
import ui.FilteredListModel
import ui.PlaceholderTextField
import ui.listener.FilterTextListener
import utils.Utils
import java.awt.Component
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JDialog
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
    private val scene: Scene
) : JDialog(owner, title) {
    private lateinit var contentPane: JPanel
    private lateinit var listLocations: JList<Location>
    private lateinit var scrollListLocations: JScrollPane
    private lateinit var txtSearchQuery: PlaceholderTextField
    private lateinit var txtRadius: JTextField
    private lateinit var lblErrorText: JLabel
    private lateinit var btnLoad: JButton
    private lateinit var filterableLocations: FilteredListModel<Location>

    init {
        setContentPane(contentPane)
        preferredSize = Dimension(600, 400)

        scrollListLocations.apply {
            val borderColor = UIManager.getColor("InternalFrame.borderColor")
            border = BorderFactory.createMatteBorder(0, 1, 0, 0, borderColor)
        }

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

        rootPane.defaultButton = btnLoad
        pack()
    }

    private fun createUIComponents() {
        filterableLocations = FilteredListModel(Location::name)
        listLocations = JList(filterableLocations).apply {
            cellRenderer = LocationCell()
        }
        txtSearchQuery = PlaceholderTextField("", "Lumbridge")
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
