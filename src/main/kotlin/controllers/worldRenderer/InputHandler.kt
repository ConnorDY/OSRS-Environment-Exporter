package controllers.worldRenderer

import models.DebugOptionsModel
import models.scene.Scene
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

class InputHandler internal constructor(
    private val camera: Camera,
    private val scene: Scene,
    private val debugOptionsModel: DebugOptionsModel,
) : KeyListener, MouseListener {

    var renderer: Renderer? = null

    var isLeftMouseDown = false
    var leftMousePressed = false
    var isRightMouseDown = false
    private val keys = BooleanArray(250)
    var mouseClicked = false
    private var previousMouseX = 0
    private var previousMouseY = 0
    var mouseX = 0
    var mouseY = 0

    fun tick(dt: Double) {
        if (dt > 1000) { // big lag spike, don't send the user flying
            return
        }
        val xVec = (-camera.yawSin).toDouble() / 65535
        val yVec = camera.yawCos.toDouble() / 65535
        val zVec = camera.pitchSin.toDouble() / 65535
        var speed = 1
        if (keys[KeyEvent.VK_SHIFT]) {
            speed = 4
        }
        if (keys[KeyEvent.VK_W]) {
            camera.addX((dt * xVec * speed).toInt())
            camera.addY((dt * yVec * speed).toInt())
            camera.addZ((dt * zVec * speed).toInt())
        }
        if (keys[KeyEvent.VK_S]) {
            camera.addX((-(dt * xVec * speed)).toInt())
            camera.addY((-(dt * yVec * speed)).toInt())
            camera.addZ((-(dt * zVec * speed)).toInt())
        }
        if (keys[KeyEvent.VK_A]) {
            // X uses yVec because we want to move perpendicular
            camera.addX((-(dt * yVec * speed)).toInt())
            camera.addY((dt * xVec * speed).toInt())
        }
        if (keys[KeyEvent.VK_D]) {
            camera.addX((dt * yVec * speed).toInt())
            camera.addY((-(dt * xVec * speed)).toInt())
        }
        if (keys[KeyEvent.VK_SPACE]) {
            camera.addZ((-dt).toInt() * speed)
        }
        if (keys[KeyEvent.VK_X]) {
            camera.addZ(dt.toInt() * speed)
        }
        if (debugOptionsModel.isDebugMode) {
            if (keys[KeyEvent.VK_J]) {
                scene.loadRadius(8014, 5)
            }
            if (keys[KeyEvent.VK_K]) {
                scene.loadRadius(13360, 3)
            }
            if (keys[KeyEvent.VK_L]) {
                scene.loadRadius(13408, 3)
            }
            if (keys[KeyEvent.VK_SEMICOLON]) {
                scene.loadRadius(12850, 3)
            }
        }
    }

    override fun keyTyped(p0: KeyEvent?) {
    }

    override fun keyPressed(e: KeyEvent) {
        val code = e.keyCode
        if (code >= 0 && code < keys.size)
            keys[code] = true
    }

    override fun keyReleased(e: KeyEvent) {
        val code = e.keyCode
        if (code >= 0 && code < keys.size)
            keys[code] = false
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
    }

    override fun mouseReleased(e: MouseEvent) {
        if (e.button == MouseEvent.BUTTON3) {
            isRightMouseDown = false
        }
        if (e.button == MouseEvent.BUTTON1) {
            isLeftMouseDown = false
        }
    }

    //TODO
//    override fun mouseMoved(e: MouseEvent) {
//        mouseX = e.x
//        mouseY = e.y
//    }
//
//    override fun mouseDragged(e: MouseEvent) {
//        if (isRightMouseDown) {
//            handleCameraDrag(e)
//        }
//        mouseX = e.x
//        mouseY = e.y
//        previousMouseX = e.x
//        previousMouseY = e.y
//    }
}
