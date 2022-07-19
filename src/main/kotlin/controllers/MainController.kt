package controllers

import JfxApplication.Companion.injector
import com.google.inject.Inject
import controllers.worldRenderer.WorldRendererController
import javafx.animation.AnimationTimer
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.stage.Stage
import javafx.util.Callback
import models.DebugModel
import org.dockfx.DockNode
import org.dockfx.DockPane
import org.dockfx.DockPos
import org.slf4j.LoggerFactory

class MainController @Inject constructor(
    private val debugModel: DebugModel
) {
    private val logger = LoggerFactory.getLogger(MainController::class.java)

    @FXML
    lateinit var menuChangeRegion: MenuItem

    @FXML
    lateinit var menuLocationSearch: MenuItem

    @FXML
    lateinit var menuAbout: MenuItem

    @FXML
    lateinit var dockPane: DockPane

    @FXML
    lateinit var lblFps: Label

    @FXML
    lateinit var btnExport: Button

    @FXML
    lateinit var z0ChkBtn: CheckBox

    @FXML
    lateinit var z1ChkBtn: CheckBox

    @FXML
    lateinit var z2ChkBtn: CheckBox

    @FXML
    lateinit var z3ChkBtn: CheckBox

    private lateinit var worldRendererControllerController: WorldRendererController

    @FXML
    fun initialize() {
        val worldLoader = FXMLLoader()
        worldLoader.controllerFactory = Callback { type: Class<*>? ->
            injector.getInstance(type)
        }

        worldLoader.location = javaClass.getResource("/views/widgets/world-renderer-widget.fxml")
        val worldRendererWidget = worldLoader.load<Parent>()
        worldRendererControllerController = worldLoader.getController()
        val worldRendererNode = DockNode(worldRendererWidget)
        worldRendererNode.setPrefSize(800.0, 600.0)
        worldRendererNode.dock(dockPane, DockPos.RIGHT)
        worldRendererNode.dockTitleBar.isVisible = false

        DockPane.initializeDefaultUserAgentStylesheet()

        // Export button handler
        btnExport.setOnAction {
            worldRendererControllerController.renderer.exportScene()
            logger.info("Exported as glTF!")
            worldRendererNode.title = "Export Completed"
            Alert(Alert.AlertType.NONE, "Exported as glTF!", ButtonType.OK).show()
        }

        // Z checkbox handlers
        z0ChkBtn.setOnAction {
            worldRendererControllerController.renderer.z0ChkBtnSelected = z0ChkBtn.isSelected
            worldRendererControllerController.renderer.isSceneUploadRequired = true
        }
        z1ChkBtn.setOnAction {
            worldRendererControllerController.renderer.z1ChkBtnSelected = z1ChkBtn.isSelected
            worldRendererControllerController.renderer.isSceneUploadRequired = true
        }
        z2ChkBtn.setOnAction {
            worldRendererControllerController.renderer.z2ChkBtnSelected = z2ChkBtn.isSelected
            worldRendererControllerController.renderer.isSceneUploadRequired = true
        }
        z3ChkBtn.setOnAction {
            worldRendererControllerController.renderer.z3ChkBtnSelected = z3ChkBtn.isSelected
            worldRendererControllerController.renderer.isSceneUploadRequired = true
        }

        // Menu item handlers
        menuChangeRegion.setOnAction {
            openWindow("region-chooser", "Region Chooser")
        }

        menuLocationSearch.setOnAction {
            openWindow("location-search", "Location Search")
        }

        menuAbout.setOnAction {
            openWindow("about", "About")
        }

        // FPS updater
        object : AnimationTimer() {
            override fun handle(now: Long) {
                lblFps.text = "FPS: ${debugModel.fps.get()}"
            }
        }.start()
    }

    fun forceRefresh() {
        worldRendererControllerController.forceRefresh()
    }

    private fun openWindow(view: String, title: String) {
        val locationSearchLoader = FXMLLoader()
        locationSearchLoader.controllerFactory = Callback { type: Class<*>? ->
            injector.getInstance(type)
        }
        locationSearchLoader.location = javaClass.getResource("/views/$view.fxml")

        val regionChangeRoot = locationSearchLoader.load<Parent>()
        val stage = Stage()

        stage.title = title
        stage.scene = javafx.scene.Scene(regionChangeRoot)
        stage.show()
    }
}
