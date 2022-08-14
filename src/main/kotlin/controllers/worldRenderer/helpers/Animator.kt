package controllers.worldRenderer.helpers

import org.lwjgl.opengl.GL
import org.lwjgl.opengl.awt.AWTGLCanvas
import javax.swing.SwingUtilities

class Animator(private val canvas: AWTGLCanvas) {
    var lastFPS = 0.0
    private var lastTime = System.nanoTime()
    private var running = false

    private inner class RenderRunnable : Runnable {
        override fun run() {
            if (!running) return
            if (!canvas.isValid) {
                GL.setCapabilities(null);
                return;
            }
            canvas.render()
            val thisTime = System.nanoTime()
            if (thisTime != lastTime)
                lastFPS = 1_000_000_000.0 / (thisTime - lastTime)
            lastTime = thisTime
            SwingUtilities.invokeLater(this)
        }
    }

    fun start() {
        running = true
        SwingUtilities.invokeLater(RenderRunnable())
    }

    fun stop() {
        running = false
    }
}
