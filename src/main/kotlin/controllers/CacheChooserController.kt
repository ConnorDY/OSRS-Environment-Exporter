package controllers

import cache.CacheLibraryProvider
import com.displee.cache.CacheLibrary
import com.google.inject.Inject
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.paint.Color
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import models.DebugModel
import models.scene.Scene

class CacheChooserController @Inject constructor(
    private val cacheLibraryProvider: CacheLibraryProvider
){

    @FXML
    private lateinit var txtRegionId: TextField
    @FXML
    private lateinit var txtRadius: TextField

    @FXML
    private lateinit var btnChooseDirectory: Button

    @FXML
    private lateinit var lblErrorText: Label

    @FXML
    private fun initialize() {
        btnChooseDirectory.setOnAction {
            val directoryChooser = DirectoryChooser()
            val f = directoryChooser.showDialog(null)
            loadCache(f.absolutePath)
        }
    }

    private fun loadCache(dir: String) {
        cacheLibraryProvider.setLibraryLocation(dir)
    }

    companion object {
        private const val INVALID_REGION_ID_TEXT = "Region Id must be between 4647 and 15522."
        private const val INVALID_RADIUS_TEXT = "Radius must be between 1 and 8."
    }
}