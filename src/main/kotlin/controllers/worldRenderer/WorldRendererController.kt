package controllers.worldRenderer

import com.google.inject.Inject
import controllers.SettingsController
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.fxml.FXML
import javafx.geometry.Bounds
import javafx.scene.Group
import javafx.scene.layout.AnchorPane
import models.Configuration
import java.util.Timer
import java.util.TimerTask

class WorldRendererController @Inject constructor(
    private val renderer: Renderer,
    private val configuration: Configuration,
) {
    @FXML
    lateinit var anchorPane: AnchorPane

    @FXML
    lateinit var group: Group

    @FXML
    fun initialize() {
        renderer.initCanvas(group)
        renderer.loadScene()
//        renderer.bindModels()
        anchorPane.boundsInLocalProperty().addListener(listener)

        renderer.window.requestFocus()
        group.requestFocus()

        checkFpsCap()
    }

    fun forceRefresh() {
        val bounds = anchorPane.localToScene(anchorPane.boundsInLocal)
        // 26 is the size of the hidden DockTitleBar
        renderer.reposResize(bounds.minX.toInt(), bounds.minY.toInt() - 26, bounds.width.toInt(), bounds.height.toInt() + 26)
    }

    private fun checkFpsCap() {
        val fpsCap = configuration.getProp(SettingsController.FPS_CAP_PROP).toIntOrNull()

        if (fpsCap != null) {
            renderer.setFpsTarget(fpsCap)
        }
    }

    // create a listener
    private val listener: ChangeListener<Bounds?> = object : ChangeListener<Bounds?> {
        val timer = Timer() // uses a timer to call your resize method
        var task: TimerTask? = null // task to execute after defined delay
        val delayTime: Long = 2 // delay that has to pass in order to consider an operation done
        override fun changed(
            observable: ObservableValue<out Bounds?>?,
            oldValue: Bounds?,
            newValue: Bounds?
        ) {
            if (task != null) { // there was already a task scheduled from the previous operation ...
                task!!.cancel() // cancel it, we have a new size to consider
            }
            task = object : TimerTask( // create new task that calls your resize operation
            ) {
                override fun run() {
                    // here you can place your resize code
                    val bounds = anchorPane.localToScene(newValue)
                    // 26 is the size of the hidden DockTitleBar
                    renderer.reposResize(bounds.minX.toInt(), bounds.minY.toInt() - 26, bounds.width.toInt(), bounds.height.toInt() + 26)
                }
            }
            // schedule new task
            timer.schedule(task, delayTime)
        }
    }
}
