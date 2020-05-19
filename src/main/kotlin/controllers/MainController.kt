package controllers

import JfxApplication.Companion.injector
import com.google.inject.Inject
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TabPane
import javafx.scene.shape.Rectangle
import javafx.util.Callback
import models.DebugModel
import org.dockfx.DockNode
import org.dockfx.DockPane
import org.dockfx.DockPos
import controllers.worldRenderer.WorldRendererController

class MainController @Inject constructor(private val debugModel: DebugModel) {

    @FXML
    lateinit var dockPane: DockPane

    @FXML
    lateinit var lblFps: Label

    @FXML
    lateinit var btnTest: Button

    private lateinit var worldRendererControllerController: WorldRendererController

    @FXML
    fun initialize() {
        val topdownLoader = FXMLLoader()
        topdownLoader.controllerFactory = Callback { type: Class<*>? ->
            injector.getInstance(type)
        }
        topdownLoader.location = javaClass.getResource("/views/widgets/topdown-map-widget.fxml")
        val topdownWidget = topdownLoader.load<Parent>()
        val topdownNode = DockNode(topdownWidget)
        topdownNode.title = "Topdown View"
        topdownNode.setPrefSize(600.0, 600.0)
        topdownNode.dock(dockPane, DockPos.LEFT)


        val worldLoader = FXMLLoader()
        worldLoader.controllerFactory = Callback { type: Class<*>? ->
            injector.getInstance(type)
        }

        worldLoader.location = javaClass.getResource("/views/widgets/world-renderer-widget.fxml")
        val worldRendererWidget = worldLoader.load<Parent>()
        worldRendererControllerController = worldLoader.getController<WorldRendererController>()
        val worldRendererNode = DockNode(worldRendererWidget)
        worldRendererNode.setPrefSize(800.0, 600.0)
        worldRendererNode.dock(dockPane, DockPos.RIGHT, topdownNode)
        worldRendererNode.dockTitleBar.isVisible = false



        // create a default test node for the center of the dock area
        val tabs = TabPane()
        val tabsDock = DockNode(tabs, "Tabs Dock")
        tabsDock.setPrefSize(300.0, 100.0)
        tabsDock.dock(dockPane, DockPos.BOTTOM)

        val rect = Rectangle(200.0, 200.0)

        val rectDock = DockNode(rect, "Rectangle")
        rectDock.dock(dockPane, DockPos.RIGHT, tabsDock)

        DockPane.initializeDefaultUserAgentStylesheet()

        btnTest.setOnAction {
            println(dockPane)
        }

        debugModel.fps.addListener { _, _, newValue ->
            Platform.runLater { lblFps.text = "FPS: %f".format(newValue) }
        }
    }

    fun forceRefresh() {
        worldRendererControllerController.forceRefresh()
    }
}