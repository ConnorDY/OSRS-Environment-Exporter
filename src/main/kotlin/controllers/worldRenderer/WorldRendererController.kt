package controllers.worldRenderer

import models.config.ConfigOption
import models.config.Configuration
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel

class WorldRendererController constructor(
    val renderer: Renderer,
    private val configuration: Configuration,
) : JPanel() {
    init {
        preferredSize = Dimension(800, 600)
        ignoreRepaint = true // we're painted by an embedded GL thing
        layout = BorderLayout()

        val canvas = renderer.initCanvas()
        canvas.let(::add)
        canvas.requestFocus()

        checkFpsCap()
        renderer.start()
    }

    fun stopRenderer() {
        renderer.stop()
    }

    private fun checkFpsCap() {
        val fpsCap = configuration.getProp(ConfigOption.fpsCap)

        if (fpsCap != null) {
            renderer.setFpsTarget(fpsCap)
        }
    }
}
