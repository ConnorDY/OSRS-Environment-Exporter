package controllers

import com.google.inject.Inject
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Stage
import models.scene.Scene
import utils.LinkHandler
import java.util.regex.Pattern

class RegionChooserController @Inject constructor(
    private val scene: Scene
) {
    @FXML
    private lateinit var txtRegionId: TextField

    @FXML
    private lateinit var txtRadius: TextField

    @FXML
    private lateinit var btnLoad: Button

    @FXML
    private lateinit var lblErrorText: Label

    @FXML
    private lateinit var linkMap: Hyperlink

    @FXML
    private fun initialize() {
        // limit Region ID input to 5 digits
        val regionIDPattern = Pattern.compile("\\d{0,5}")
        val regionIDFormatter = TextFormatter<String> { change ->
            if (regionIDPattern.matcher(change.controlNewText).matches()) {
                return@TextFormatter change
            } else {
                return@TextFormatter null
            }
        }
        txtRegionId.textFormatter = regionIDFormatter

        // limit Radius input to 2 digits
        val radiusPattern = Pattern.compile("\\d{0,2}")
        val radiusFormatter = TextFormatter<String> { change ->
            if (radiusPattern.matcher(change.controlNewText).matches()) {
                return@TextFormatter change
            } else {
                return@TextFormatter null
            }
        }
        txtRadius.textFormatter = radiusFormatter

        // Load Region handler
        btnLoad.setOnAction {
            lblErrorText.isVisible = false
            val regionId: Int? = txtRegionId.text.toIntOrNull()
            if (regionId == null || (regionId < 4647 || regionId > 15522)) {
                lblErrorText.text = INVALID_REGION_ID_TEXT
                lblErrorText.isVisible = true
                return@setOnAction
            }

            val radius: Int? = txtRadius.text.toIntOrNull()
            if (radius == null || (radius < 1 || radius > 20)) {
                lblErrorText.text = INVALID_RADIUS_TEXT
                lblErrorText.isVisible = true
                return@setOnAction
            }
            (btnLoad.scene.window as Stage).close()
            scene.load(regionId, radius)
        }

        // explv map link handler
        linkMap.setOnAction {
            LinkHandler(linkMap.text).openInBrowser()
        }
    }

    @FXML
    private fun handleKeyPressed(e: KeyEvent) {
        if (e.code == KeyCode.ENTER)
            btnLoad.fire()
    }

    companion object {
        private const val INVALID_REGION_ID_TEXT = "Region Id must be between 4647 and 15522."
        private const val INVALID_RADIUS_TEXT = "Radius must be between 1 and 20."
    }
}
