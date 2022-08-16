package controllers.worldRenderer.helpers

import org.lwjgl.opengl.GL
import org.lwjgl.opengl.awt.AWTGLCanvas
import java.util.concurrent.Semaphore
import javax.swing.SwingUtilities

class Animator(private val canvas: AWTGLCanvas) {
    var lastFPS = 0.0
    private var startFrameTime: Long = 0
    private var lastEndFrameTime: Long = 0
    private val hiResTimerThread = Thread(HiResTimerRunnable(RenderRunnable())).apply {
        // If every other thread terminates and this is still running, something is wrong
        isDaemon = true
    }
    private var running = false
    private var deltaTimeTarget = 0
    private val syncSemaphore = Semaphore(0)

    private inner class RenderRunnable : Runnable {
        override fun run() {
            try {
                if (!running) return
                if (!canvas.isValid) {
                    GL.setCapabilities(null)
                    return
                }
                canvas.render()
            } finally {
                syncSemaphore.release()
                // If we have 2 permits, something has started the runnable twice erroneously
                assert(syncSemaphore.availablePermits() <= 1)
            }
        }
    }

    // Swing's Timer is not high resolution enough to feel nice when limiting FPS.
    // It is millisecond-resolution, but we want nanosecond resolution.
    private inner class HiResTimerRunnable(val timerTick: Runnable) : Runnable {
        override fun run() {
            // No initial delay
            SwingUtilities.invokeLater(timerTick)

            try {
                while (true) {
                    // Wait for render task to finish
                    syncSemaphore.acquire()

                    // Wait long enough to bring FPS down to target levels
                    val endFrameTime = System.nanoTime()
                    lastFPS = 1_000_000_000.0 / (endFrameTime - lastEndFrameTime)
                    val sleepTime = deltaTimeTarget + (startFrameTime - endFrameTime)
                    lastEndFrameTime = endFrameTime
                    startFrameTime = if (sleepTime in 0..SECOND_IN_NANOS) {
                        Thread.sleep(
                            sleepTime / MILLISECOND_IN_NANOS,
                            (sleepTime % MILLISECOND_IN_NANOS).toInt()
                        )
                        endFrameTime + sleepTime
                    } else {
                        endFrameTime
                    }

                    // Re-queue render task
                    SwingUtilities.invokeLater(timerTick)
                }
            } catch (e: InterruptedException) {
                // This thread should terminate on interrupt signal
                // so, drop off the end of the run function
            }
        }
    }

    /** Sets the FPS target for this animator.
     *  It may vary above and below the actual value.
     *  @param target The FPS target, or 0 for unlimited.
     */
    fun setFpsTarget(target: Int) {
        deltaTimeTarget =
            if (target > 0) SECOND_IN_NANOS / target
            else 0
    }

    fun start() {
        running = true
        hiResTimerThread.start()
    }

    fun stop() {
        running = false
        hiResTimerThread.interrupt()
    }

    companion object {
        const val SECOND_IN_NANOS = 1_000_000_000
        const val MILLISECOND_IN_NANOS = 1_000_000
    }
}
