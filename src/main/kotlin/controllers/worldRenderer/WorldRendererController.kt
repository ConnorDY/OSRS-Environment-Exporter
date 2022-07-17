package controllers.worldRenderer

import com.google.inject.Inject
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.fxml.FXML
import javafx.geometry.Bounds
import javafx.scene.Group
import javafx.scene.layout.AnchorPane
import java.util.*

class WorldRendererController @Inject constructor(val renderer: Renderer) {
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
    }

    fun forceRefresh() {
        val bounds = anchorPane.localToScene(anchorPane.boundsInLocal)
        // 26 is the size of the hidden DockTitleBar
        renderer.reposResize(bounds.minX.toInt(), bounds.minY.toInt() - 26, bounds.width.toInt(), bounds.height.toInt() + 26)
    }

    // create a listener
    private val listener: ChangeListener<Bounds?> = object : ChangeListener<Bounds?> {
        val timer: Timer = Timer() // uses a timer to call your resize method
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
