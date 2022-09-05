package controllers.worldRenderer

import kotlin.math.abs

class Camera {
    var yaw = 0 // yaw 0 true north, same with 2047 and 1
    var pitch = 220
    val scale = 550
    var cameraX: Double = 0.0
    var cameraY: Double = 0.0
    var cameraZ: Double = -2500.0
    var centerX = 400 // HALF OF VIEWPORT!
    var centerY = 300 // HALF OF VIEWPORT!
    var motionTicks = 0 // Amount of times this camera has been moved
    fun addX(amt: Double) {
        cameraX += amt
    }

    fun addY(amt: Double) {
        cameraY += amt
    }

    fun addZ(amt: Double) {
        cameraZ += amt
    }

    fun addYaw(amt: Int) {
        var newYaw = yaw + amt
        if (newYaw > 2047) {
            newYaw = 0
        }
        if (newYaw < 0) {
            newYaw = 2047
        }
        yaw = newYaw
    }

    fun addPitch(amt: Int) {
        if (abs(amt) > 0x400) return

        var newPitch = pitch + amt
        // straight down is 0x200, straight up is 0x600 roughly
        if (newPitch > 0x200 && newPitch < 0x600) {
            if (amt > 0) {
                newPitch = 0x200
            } else {
                newPitch = 0x600
            }
        }
        if (newPitch < 0) {
            newPitch += 2048
        }
        if (newPitch > 2047) {
            newPitch -= 2048
        }
        pitch = newPitch
    }

    val pitchRads: Double get() = pitch * Constants.UNIT
    val yawRads: Double get() = yaw * Constants.UNIT

    val pitchSin: Int
        get() = Constants.SINE.get(pitch)

    val pitchCos: Int
        get() = Constants.COSINE.get(pitch)

    val yawSin: Int
        get() = Constants.SINE.get(yaw)

    val yawCos: Int
        get() = Constants.COSINE.get(yaw)
}
