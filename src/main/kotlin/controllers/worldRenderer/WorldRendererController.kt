package controllers.worldRenderer

import com.jogamp.newt.awt.NewtCanvasAWT
import controllers.SettingsController
import models.Configuration
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JPanel

class WorldRendererController constructor(
    val renderer: Renderer,
    private val configuration: Configuration,
) : JPanel() {
    private val canvas: NewtCanvasAWT
    init {
        preferredSize = Dimension(800, 600)
        maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)

        canvas = renderer.initCanvas()
        canvas.let(::add)

        addComponentListener(Listener())

        forceRefresh()
        renderer.window.requestFocus()

        checkFpsCap()
    }

    fun loadScene() = renderer.loadScene()

    fun stopRenderer() {
        renderer.stop()
    }

    fun forceRefresh() {
        val bounds = size
        canvas.bounds = Rectangle(0, 0, bounds.width, bounds.height)
    }

    private fun checkFpsCap() {
        val fpsCap = configuration.getProp(SettingsController.FPS_CAP_PROP).toIntOrNull()

        if (fpsCap != null) {
            renderer.setFpsTarget(fpsCap)
        }
    }

    private inner class Listener : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
            forceRefresh()
        }

        override fun componentShown(e: ComponentEvent?) {
            forceRefresh()
        }
    }
}
