package controllers

import JfxApplication.Companion.injector
import cache.loaders.LocationsLoader
import com.displee.cache.CacheLibrary
import com.google.inject.Inject
import controllers.worldRenderer.WorldRendererController
import javafx.animation.AnimationTimer
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.util.Callback
import models.DebugModel
import models.scene.Scene
import org.dockfx.DockNode
import org.dockfx.DockPane
import org.dockfx.DockPos
import java.io.File

class MainController @Inject constructor(
    private val debugModel: DebugModel,
    private val scene: Scene,
    private val locationsLoader: LocationsLoader
) {

    @FXML
    lateinit var dockPane: DockPane

    @FXML
    lateinit var lblFps: Label

    @FXML
    lateinit var btnTest: Button

    @FXML
    lateinit var btnPlace: Button

    @FXML
    lateinit var btnDelete: Button

    private lateinit var worldRendererControllerController: WorldRendererController

    @FXML
    fun initialize() {
//        val topdownLoader = FXMLLoader()
//        topdownLoader.controllerFactory = Callback { type: Class<*>? ->
//            injector.getInstance(type)
//        }
//        topdownLoader.location = javaClass.getResource("/views/widgets/topdown-map-widget.fxml")
//        val topdownWidget = topdownLoader.load<Parent>()
//        val topdownNode = DockNode(topdownWidget)
//        topdownNode.title = "Topdown View"
//        topdownNode.setPrefSize(600.0, 600.0)
//        topdownNode.dock(dockPane, DockPos.LEFT)



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




        val objectPickerLoader = FXMLLoader()
        objectPickerLoader.controllerFactory = Callback { type: Class<*>? ->
            injector.getInstance(type)
        }
        objectPickerLoader.location = javaClass.getResource("/views/object-picker.fxml")
        val objectPickerNode = DockNode(objectPickerLoader.load<Parent>())
        objectPickerNode.title = "Object Picker"
        objectPickerNode.setPrefSize(400.0, 400.0)
        objectPickerNode.dock(dockPane, DockPos.RIGHT, worldRendererNode)


        DockPane.initializeDefaultUserAgentStylesheet()

        btnTest.setOnAction {
//            File("cache-181").copyRecursively(File("cache-out"), true)
            val sr = scene.getRegion(0,0)!!
//            sr.locationsDefinition.locations.removeIf { it.type == 10 || it.type == 11 || it.type == 22 }
            locationsLoader.writeLocations(CacheLibrary("cache-out"), sr.locationsDefinition)
            println("saved")
        }

        object : AnimationTimer() {
            override fun handle(now: Long) {
                lblFps.text = "FPS: %f".format(debugModel.fps.get())
            }
        }.start()
    }

    fun forceRefresh() {
        worldRendererControllerController.forceRefresh()
    }
}