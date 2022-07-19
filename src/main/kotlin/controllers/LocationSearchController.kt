package controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.transformation.FilteredList
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.text.TextAlignment
import javafx.stage.Stage
import models.locations.Location
import models.locations.Locations
import models.scene.Scene
import utils.Utils
import java.util.regex.Pattern

class LocationSearchController @Inject constructor(
    private val scene: Scene
) {
    @FXML
    lateinit var wrapper: AnchorPane

    @FXML
    lateinit var txtSearchQuery: TextField

    @FXML
    lateinit var listLocations: ListView<Location>

    @FXML
    lateinit var txtRadius: TextField

    @FXML
    private lateinit var btnLoad: Button

    @FXML
    private lateinit var lblErrorText: Label

    private var selectedLocation: Location? = null

    @FXML
    private fun handleKeyPressed(e: KeyEvent) {
        if (e.code == KeyCode.ENTER)
            btnLoad.fire()
    }

    @FXML
    private fun initialize() {
        listLocations.setCellFactory { LocationCell() }

        // setup list placeholder while loading
        val listLocationsPlacholder = Label("Loading locations...")
        listLocationsPlacholder.textAlignment = TextAlignment.CENTER
        listLocations.placeholder = listLocationsPlacholder

        // load locations
        val locationMapper = ObjectMapper()

        val _locations =
            locationMapper.readValue(
                this::class.java.getResource("/data/locations.json"),
                Locations::class.java
            )

        val locations = FXCollections.observableArrayList<Location>()
        locations.addAll(_locations.locations)

        val filterableLocations = FilteredList(locations)
        listLocations.items = filterableLocations

        // list view selection handler
        listLocations.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                selectedLocation = newValue
                btnLoad.isDisable = false
            }
        }

        // limit Radius input to 2 digits
        val radiusPattern = Pattern.compile("\\d{0,2}")
        val radiusFormatter = TextFormatter<String> { change ->
            if (radiusPattern.matcher(change.controlNewText).matches()) change else null
        }
        txtRadius.textFormatter = radiusFormatter

        // search handler
        txtSearchQuery.textProperty().addListener { _: ObservableValue<out String>?, _: String?, newVal: String ->
            val newValLowercase = newVal.lowercase()
            filterableLocations.setPredicate { location ->
                location.name.lowercase().contains(newValLowercase)
            }
        }

        // load button handler
        btnLoad.setOnAction {
            val regionId = regionIdForLocation(selectedLocation!!)

            val radius: Int? = txtRadius.text.toIntOrNull()
            if (radius == null || (radius < 1 || radius > 20)) {
                lblErrorText.text = RegionChooserController.INVALID_RADIUS_TEXT
                lblErrorText.isVisible = true
                return@setOnAction
            }
            (wrapper.scene.window as Stage).close()
            scene.load(regionId, radius)
        }
    }

    private class LocationCell : ListCell<Location>() {
        override fun updateItem(item: Location?, empty: Boolean) {
            super.updateItem(item, empty)

            if (empty || item == null) {
                text = null
                graphic = null
            } else {
                val regionId = regionIdForLocation(item)
                this.text = "${item.name} ($regionId)"
            }
        }
    }

    companion object {
        fun regionIdForLocation(location: Location): Int {
            val (x, y) = location.coords
            return Utils.worldCoordinatesToRegionId(x, y)
        }
    }
}
