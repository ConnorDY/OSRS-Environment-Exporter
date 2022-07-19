package controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.text.TextAlignment
import javafx.util.Callback
import models.locations.Location
import models.locations.Locations
import models.scene.Scene

class LocationSearchController @Inject constructor(
    private val scene: Scene
) {
    @FXML
    lateinit var txtSearchPrompt: TextField

    @FXML
    lateinit var listLocations: ListView<Location>

    @FXML
    private lateinit var btnLoad: Button

    private var locations = FXCollections.observableArrayList<Location>()

    @FXML
    private fun handleKeyPressed(e: KeyEvent) {
        if (e.code == KeyCode.ENTER)
            btnLoad.fire()
    }

    @FXML
    private fun initialize() {
        listLocations.cellFactory = LocationCellFactory()
        listLocations.items = locations

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

        locations.addAll(_locations.locations)
    }

    private class LocationCell : ListCell<Location>() {
        override fun updateItem(item: Location?, empty: Boolean) {
            super.updateItem(item, empty)

            if (empty || item == null) {
                text = null
                graphic = null
            } else {
                this.text = item.name
            }
        }
    }

    private class LocationCellFactory : Callback<ListView<Location>, ListCell<Location>> {
        override fun call(listview: ListView<Location>): ListCell<Location> {
            return LocationCell()
        }
    }
}
