package controllers

import com.google.inject.Inject
import controllers.worldRenderer.Renderer
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import javafx.scene.control.TextFormatter
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import models.Configuration
import java.util.regex.Pattern

class SettingsController @Inject constructor(
    private val renderer: Renderer,
    private val configuration: Configuration
) {
    @FXML
    lateinit var wrapper: AnchorPane

    @FXML
    lateinit var chkBoxLimitFps: CheckBox

    @FXML
    lateinit var txtFpsCap: TextField

    @FXML
    lateinit var btnSave: Button

    @FXML
    private fun initialize() {
        // load current setting(s)
        val fpsCap = configuration.getProp(FPS_CAP_PROP).toIntOrNull()

        if (fpsCap != null) {
            txtFpsCap.text = fpsCap.toString()
            chkBoxLimitFps.isSelected = true
        } else {
            txtFpsCap.isDisable = true
        }

        // FPS Limit checkbox handler
        chkBoxLimitFps.setOnAction {
            txtFpsCap.isDisable = !chkBoxLimitFps.isSelected
        }

        // limit FPS Cap input to 3 digits
        val fpsCapPattern = Pattern.compile("\\d{0,3}")
        val fpsCapFormatter = TextFormatter<String> { change ->
            if (fpsCapPattern.matcher(change.controlNewText).matches()) change else null
        }
        txtFpsCap.textFormatter = fpsCapFormatter

        // Save Preferences button handler
        btnSave.setOnAction {
            configuration.saveProp(FPS_CAP_PROP, if (chkBoxLimitFps.isSelected) txtFpsCap.text else "")

            var fpsCapToSet = txtFpsCap.text.toIntOrNull() ?: 0
            if (chkBoxLimitFps.isDisable) fpsCapToSet = 0

            renderer.setFpsTarget(fpsCapToSet)

            (btnSave.scene.window as Stage).close()
        }
    }

    companion object {
        const val FPS_CAP_PROP = "fps-cap"
    }
}
