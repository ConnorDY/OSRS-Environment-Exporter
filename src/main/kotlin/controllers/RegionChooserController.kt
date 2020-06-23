package controllers

import com.google.inject.Inject
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.paint.Color
import javafx.stage.Stage
import models.DebugModel
import models.scene.Scene
import java.awt.Desktop
import java.net.URI

class RegionChooserController @Inject constructor(
    private val scene: Scene
){

    @FXML
    private lateinit var txtRegionId: TextField
    @FXML
    private lateinit var txtRadius: TextField

    @FXML
    private lateinit var btnLoad: Button

    @FXML
    private lateinit var lblErrorText: Label

    @FXML
    private lateinit var linkExplv: Hyperlink

    @FXML
    private fun initialize() {
        btnLoad.setOnAction {
            lblErrorText.isVisible = false
            val regionId: Int? = txtRegionId.text.toIntOrNull()
            if (regionId == null ||(regionId < 4647 || regionId > 15522)) {
                lblErrorText.text = INVALID_REGION_ID_TEXT
                lblErrorText.isVisible = true
                return@setOnAction
            }

            val radius: Int? = txtRadius.text.toIntOrNull()
            if (radius == null || (radius < 1 || radius > 8)) {
                lblErrorText.text = INVALID_RADIUS_TEXT
                lblErrorText.isVisible = true
                return@setOnAction
            }
            (btnLoad.scene.window as Stage).close()
            scene.load(regionId, radius)
        }

        linkExplv.setOnAction {
            Desktop.getDesktop().browse(URI("https://explv.github.io/"))
        }
    }

    @FXML
    private fun handleKeyPressed(e: KeyEvent) {
        if (e.code == KeyCode.ENTER)
            btnLoad.fire()
    }

    companion object {
        private const val INVALID_REGION_ID_TEXT = "Region Id must be between 4647 and 15522."
        private const val INVALID_RADIUS_TEXT = "Radius must be between 1 and 8."
    }
}