package controllers.worldRenderer.helpers

import models.FrameRateModel
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.awt.AWTGLCanvas
import utils.Utils.doAllActions
import utils.Utils.isMacOS
import java.util.Collections.synchronizedList
import java.util.concurrent.ConcurrentLinkedQueue
import javax.swing.SwingUtilities

class Animator(private val canvas: AWTGLCanvas, private val frameRateModel: FrameRateModel) {
    private var startFrameTime: Long = 0
    private val hiResTimerThread = Thread(HiResTimerRunnable(), "Animator").apply {
        // If every other thread terminates and this is still running, something is wrong
        isDaemon = true
    }
    @Volatile
    private var running = false
    @Volatile
    private var deltaTimeTarget = 0

    @Volatile
    var terminateCallback: Runnable? = null
    private val temporaryPreRenderListeners = ConcurrentLinkedQueue<Runnable>()
    private val preRenderListeners: MutableList<Runnable> = synchronizedList(ArrayList<Runnable>())

    // Swing's Timer is not high resolution enough to feel nice when limiting FPS.
    // It is millisecond-resolution, but we want nanosecond resolution.
    private inner class HiResTimerRunnable : Runnable {
        override fun run() {
            try {
                while (true) {
                    if (!running) return

                    temporaryPreRenderListeners.doAllActions()
                    preRenderListeners.forEach(Runnable::run)

                    if (isMacOS()) {
                        SwingUtilities.invokeAndWait(::callRender)
                    } else {
                        callRender()
                    }

                    // Wait long enough to bring FPS down to target levels
                    val endFrameTime = System.nanoTime()
                    frameRateModel.frameCount++
                    val sleepTime = deltaTimeTarget + (startFrameTime - endFrameTime)
                    startFrameTime = if (sleepTime in 0..SECOND_IN_NANOS) {
                        Thread.sleep(
                            sleepTime / MILLISECOND_IN_NANOS,
                            (sleepTime % MILLISECOND_IN_NANOS).toInt()
                        )
                        endFrameTime + sleepTime
                    } else {
                        endFrameTime
                    }

                    synchronized(frameRateModel.updateNotifier) {
                        if (frameRateModel.powerSavingMode.get() && !frameRateModel.needAnotherFrame) {
                            frameRateModel.updateNotifier.wait()
                        }
                        frameRateModel.needAnotherFrame = false
                    }

                    SwingUtilities.invokeAndWait {
                        // Hold up the thread until swing ticks
                        // this means we don't contend for the swing lock too much when our frame rate is very high
                        // this is also important because the GL thread will sleep with the AWT lock held when
                        // waiting for a vertical blank, which would otherwise cause swing events to be dropped
                    }
                }
            } catch (e: InterruptedException) {
                // This thread should terminate on interrupt signal
                // so, drop off the end of the run function
            }

            terminateCallback?.run()
        }

        private fun callRender() {
            if (canvas.isValid) {
                canvas.render()
            } else {
                GL.setCapabilities(null)
            }
        }
    }

    fun checkGlThread() {
        if (Thread.currentThread() != hiResTimerThread) {
            throw IllegalStateException("This method must be called from the GL thread")
        }
    }

    fun doOnceBeforeGlRender(action: Runnable) {
        temporaryPreRenderListeners.add(action)
    }

    fun doBeforeGlRender(action: Runnable) {
        preRenderListeners.add(action)
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

    fun stopWith(callback: Runnable) {
        terminateCallback = callback
        running = false
        hiResTimerThread.interrupt()
    }

    companion object {
        const val SECOND_IN_NANOS = 1_000_000_000
        const val MILLISECOND_IN_NANOS = 1_000_000
    }
}
