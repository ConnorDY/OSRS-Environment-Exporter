package controllers.worldRenderer

import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JPanel

class WorldRendererController(val renderer: Renderer) : JPanel() {
    init {
        preferredSize = Dimension(800, 600)
        ignoreRepaint = true // we're painted by an embedded GL thing
        layout = BorderLayout()

        val canvas = renderer.initCanvas()
        canvas.let(::add)
        canvas.requestFocus()

        renderer.start()
    }

    fun stopRenderer() {
        renderer.stop()
    }
}
