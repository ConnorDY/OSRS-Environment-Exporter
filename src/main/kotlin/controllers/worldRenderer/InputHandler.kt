package controllers.worldRenderer

import models.FrameRateModel
import models.config.ConfigOptions
import models.scene.Scene
import java.awt.Component
import java.awt.GraphicsEnvironment
import java.awt.Robot
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener

class InputHandler internal constructor(
    private val parent: Component,
    private val camera: Camera,
    private val scene: Scene,
    private val configOptions: ConfigOptions,
    private val frameRateModel: FrameRateModel,
) : KeyListener, MouseListener, MouseMotionListener {
    private var robotScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
    private var robot = Robot(robotScreen)

    var isLeftMouseDown = false
    var leftMousePressed = false
    var isRightMouseDown = false
    private val keys = ByteArray(0x100)
    var mouseClicked = false
    private var discardUntil = System.currentTimeMillis()
    private var previousMouseX = 0
    private var previousMouseY = 0
    var mouseX = 0
    var mouseY = 0

    fun tick(dt: Double) {
        if (dt > 1000) { // big lag spike, don't send the user flying
            frameRateModel.notifyNeedFrames()
            return
        }
        val xVec = (-camera.yawSin).toDouble() / 65535
        val yVec = camera.yawCos.toDouble() / 65535
        val zVec = camera.pitchSin.toDouble() / 65535
        var speed = 1
        if (isKeyHeld(KeyEvent.VK_SHIFT)) {
            speed = 4
        }
        var motionTicked = false
        if (isKeyHeld(KeyEvent.VK_W)) {
            camera.addX((dt * xVec * speed).toInt())
            camera.addY((dt * yVec * speed).toInt())
            camera.addZ((dt * zVec * speed).toInt())
            motionTicked = true
        }
        if (isKeyHeld(KeyEvent.VK_S)) {
            camera.addX((-(dt * xVec * speed)).toInt())
            camera.addY((-(dt * yVec * speed)).toInt())
            camera.addZ((-(dt * zVec * speed)).toInt())
            motionTicked = true
        }
        if (isKeyHeld(KeyEvent.VK_A)) {
            // X uses yVec because we want to move perpendicular
            camera.addX((-(dt * yVec * speed)).toInt())
            camera.addY((dt * xVec * speed).toInt())
            motionTicked = true
        }
        if (isKeyHeld(KeyEvent.VK_D)) {
            camera.addX((dt * yVec * speed).toInt())
            camera.addY((-(dt * xVec * speed)).toInt())
            motionTicked = true
        }
        if (isKeyHeld(KeyEvent.VK_SPACE)) {
            camera.addZ((-dt).toInt() * speed)
            motionTicked = true
        }
        if (isKeyHeld(KeyEvent.VK_X)) {
            camera.addZ(dt.toInt() * speed)
            motionTicked = true
        }
        if (motionTicked) {
            camera.motionTicks++
            frameRateModel.notifyNeedFrames()
        }
    }

    private fun isKeyHeld(code: Int): Boolean {
        if (keys[code] == STATE_RELEASED) return false
        if (keys[code] == STATE_HELD || !frameRateModel.powerSavingMode.get()) return true
        keys[code] = STATE_HELD
        frameRateModel.notifyNeedFrames()
        return false
    }

    override fun keyTyped(p0: KeyEvent?) {
    }

    override fun keyPressed(e: KeyEvent) {
        val code = e.keyCode
        if (code >= 0 && code < keys.size)
            keys[code] = STATE_PRESSED

        if (configOptions.debug.value.get()) {
            when (code) {
                KeyEvent.VK_J -> scene.loadRadius(8014, 5)
                KeyEvent.VK_K -> scene.loadRadius(13360, 5)
                KeyEvent.VK_L -> scene.loadRadius(13408, 5)
                KeyEvent.VK_SEMICOLON -> scene.loadRadius(12850, 5)
            }
        }

        frameRateModel.notifyNeedFrames()
    }

    override fun keyReleased(e: KeyEvent) {
        val code = e.keyCode
        if (code >= 0 && code < keys.size)
            keys[code] = STATE_RELEASED

        frameRateModel.notifyNeedFrames()
    }

    private fun handleCameraDrag(e: MouseEvent) {
        val dx = previousMouseX - e.x
        camera.addYaw(dx)

        val dy = previousMouseY - e.y
        camera.addPitch(-dy)
    }

    override fun mouseClicked(e: MouseEvent) {
        if (e.button == MouseEvent.BUTTON1) {
            mouseClicked = true
        }
    }

    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}
    override fun mousePressed(e: MouseEvent) {
        if (e.button == MouseEvent.BUTTON3) {
            isRightMouseDown = true
            previousMouseX = e.x
            previousMouseY = e.y
        }
        if (e.button == MouseEvent.BUTTON1) {
            leftMousePressed = true
            isLeftMouseDown = true
        }
        frameRateModel.notifyNeedFrames()
    }

    override fun mouseReleased(e: MouseEvent) {
        if (e.button == MouseEvent.BUTTON3) {
            isRightMouseDown = false
        }
        if (e.button == MouseEvent.BUTTON1) {
            isLeftMouseDown = false
        }
    }

    private fun warpMouse(x: Int, y: Int) {
        val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.find { device ->
            device.configurations.find { it.bounds.contains(x, y) } != null
        }

        if (screen != null) {
            if (screen != robotScreen) {
                robotScreen = screen
                robot = Robot(screen)
            }
            robot.mouseMove(x, y)
        }
    }

    override fun mouseMoved(e: MouseEvent) {
        mouseX = e.x
        mouseY = e.y
    }

    override fun mouseDragged(e: MouseEvent) {
        if (e.`when` <= discardUntil) {
            return
        }

        var x = e.x
        var y = e.y

        if (isRightMouseDown) {
            handleCameraDrag(e)

            if (configOptions.mouseWarping.value.get()) {
                // Mouse warping
                var warp = false
                val offsetX = e.xOnScreen - e.x
                val offsetY = e.yOnScreen - e.y
                val width = parent.width - 1
                val height = parent.height - 1

                if (x >= width) {
                    warp = true
                    x = 1
                } else if (x <= 0) {
                    warp = true
                    x = width - 1
                }
                if (y >= height) {
                    warp = true
                    y = 1
                } else if (y <= 0) {
                    warp = true
                    y = height - 1
                }

                if (warp) {
                    warpMouse(offsetX + x, offsetY + y)
                    discardUntil = System.currentTimeMillis()
                }
            }

            frameRateModel.notifyNeedFrames()
        }
        mouseX = x
        mouseY = y
        previousMouseX = x
        previousMouseY = y
    }

    companion object {
        val STATE_RELEASED = 0.toByte()
        val STATE_PRESSED = 1.toByte()
        val STATE_HELD = 2.toByte()
    }
}
