package controllers.worldRenderer

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.MouseEvent
import com.jogamp.newt.event.MouseListener
import javafx.animation.AnimationTimer
import javafx.scene.input.KeyCode
import jogamp.newt.driver.DriverClearFocus
import models.ObjectsModel
import models.scene.Scene
import models.scene.SceneObject
import javax.inject.Inject

class InputHandler @Inject internal constructor(
    private val camera: Camera,
    private val scene: Scene,
    private val objectsModel: ObjectsModel
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

    private fun handleKeys(dt: Double) {
        if (dt > 1000) { // big lag spike, don't send the user flying
            return
        }
        val xVec = (-camera.yawSin).toDouble() / 65535
        val yVec = camera.yawCos.toDouble() / 65535
        val zVec = camera.pitchSin.toDouble() / 65535
        var speed = 1
        if (keys[KeyEvent.VK_SHIFT.toInt()]) {
            speed = 4
        }
        if (keys[KeyEvent.VK_W.toInt()]) {
            camera.addX((dt * xVec * speed).toInt())
            camera.addY((dt * yVec * speed).toInt())
            camera.addZ((dt * zVec * speed).toInt())
        }
        if (keys[KeyEvent.VK_S.toInt()]) {
            camera.addX((-(dt * xVec * speed)).toInt())
            camera.addY((-(dt * yVec * speed)).toInt())
            camera.addZ((-(dt * zVec * speed)).toInt())
        }
        if (keys[KeyEvent.VK_A.toInt()]) {
            // X uses yVec because we want to move perpendicular
            camera.addX((-(dt * yVec * speed)).toInt())
            camera.addY((dt * xVec * speed).toInt())
        }
        if (keys[KeyEvent.VK_D.toInt()]) {
            camera.addX((dt * yVec * speed).toInt())
            camera.addY((-(dt * xVec * speed)).toInt())
        }
        if (keys[KeyEvent.VK_SPACE.toInt()]) {
            camera.addZ((-dt).toInt() * speed)
        }
        if (keys[KeyEvent.VK_X.toInt()]) {
            camera.addZ(dt.toInt() * speed)
        }
        if (keys[KeyEvent.VK_K.toInt()]) {
            scene.load(13360, 5)
        }
        if (keys[KeyEvent.VK_L.toInt()]) {
            scene.load(13408, 5)
        }
        if (keys[KeyEvent.VK_R.toInt()]) {
            keys[KeyEvent.VK_R.toInt()] = false // debounce
            val heldObject = objectsModel.heldObject.get()
            objectsModel.heldObject.set(SceneObject(heldObject.objectDefinition, heldObject.type, heldObject.orientation + 1))
        }
        if (keys[KeyEvent.VK_ESCAPE.toInt()]) {
            objectsModel.heldObject.set(null)
        }
    }

    fun isKeyDown(keyCode: KeyCode): Boolean {
        return keys[keyCode.code]
    }

    override fun keyPressed(e: KeyEvent) {
        keys[e.keyCode.toInt()] = true
    }

    override fun keyReleased(e: KeyEvent) {
        if (e.isAutoRepeat) {
            return
        }
        keys[e.keyCode.toInt()] = false
    }

    private fun handleCameraDrag(e: MouseEvent) {
        // TODO: use screen size or adjustable speeds
        val xSpeed = (renderer!!.canvasWidth * 0.0025).toInt()
        if (previousMouseX < e.x) {
            camera.addYaw(-xSpeed)
        } else if (previousMouseX > e.x) {
            camera.addYaw(xSpeed)
        }
        val ySpeed = (renderer!!.canvasHeight * 0.002).toInt()
        if (previousMouseY < e.y) {
            camera.addPitch(ySpeed)
        } else if (previousMouseY > e.y) {
            camera.addPitch(-ySpeed)
        }
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

    override fun mouseMoved(e: MouseEvent) {
        mouseX = e.x
        mouseY = e.y
    }

    override fun mouseDragged(e: MouseEvent) {
        if (isRightMouseDown) {
            handleCameraDrag(e)
        }
        mouseX = e.x
        mouseY = e.y
        previousMouseX = e.x
        previousMouseY = e.y
    }

    override fun mouseWheelMoved(e: MouseEvent) {}

    init {
        object : AnimationTimer() {
            var lastNanoTime = System.nanoTime()
            override fun handle(now: Long) {
                val dt = (now - lastNanoTime) / 1000000.toDouble()
                lastNanoTime = now
                handleKeys(dt)
            }
        }.start()
    }
}