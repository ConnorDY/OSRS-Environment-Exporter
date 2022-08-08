package controllers.worldRenderer

import com.jogamp.newt.awt.NewtCanvasAWT
import com.jogamp.newt.event.MouseAdapter
import com.jogamp.newt.event.MouseEvent
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLEventListener
import controllers.SettingsController
import models.Configuration
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class WorldRendererController constructor(
    val renderer: Renderer,
    private val configuration: Configuration,
) : JPanel() {
    private val canvas: NewtCanvasAWT
    init {
        preferredSize = Dimension(800, 600)
        ignoreRepaint = true // we're painted by an embedded GL thing

        canvas = renderer.initCanvas()
        canvas.let(::add)

        addComponentListener(Listener())

        renderer.window.apply {
            requestFocus()
            addGLEventListener(GLInitialResizeHack())
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(p0: MouseEvent?) {
                    requestFocus()
                }
            })
        }

        checkFpsCap()
    }

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

    private inner class GLInitialResizeHack : GLEventListener {
        private var frames = 0

        override fun init(p0: GLAutoDrawable?) {}
        override fun dispose(p0: GLAutoDrawable?) {}
        override fun display(p0: GLAutoDrawable?) {
            if (frames++ == 1) {
                renderer.window.removeGLEventListener(this)
                SwingUtilities.invokeLater {
                    forceRefresh()
                }
            }
        }
        override fun reshape(p0: GLAutoDrawable?, p1: Int, p2: Int, p3: Int, p4: Int) {}
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
