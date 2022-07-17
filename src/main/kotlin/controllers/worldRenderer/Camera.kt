package controllers.worldRenderer

class Camera {
    var yaw = 0 // yaw 0 true north, same with 2047 and 1
    var pitch = 220
    val scale = 550
    var cameraX: Int = 0
    var cameraY: Int = 0
    var cameraZ = -2500
    var centerX = 400 // HALF OF VIEWPORT!
    var centerY = 300 // HALF OF VIEWPORT!
    fun addX(amt: Int) {
        cameraX += amt
    }

    fun addY(amt: Int) {
        cameraY += amt
    }

    fun addZ(amt: Int) {
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
        var newPitch = pitch + amt
        // If camera goes below 434 tile hover stops working
        // straight down is 434, straight up is 1500 roughly
        if (newPitch > 434 && newPitch < 1500) {
            return
        }
        if (newPitch < 0) {
            newPitch = 2047
        }
        if (newPitch > 2047) {
            newPitch = 0
        }
        pitch = newPitch
    }

    val pitchSin: Int
        get() = Constants.SINE.get(pitch)

    val pitchCos: Int
        get() = Constants.COSINE.get(pitch)

    val yawSin: Int
        get() = Constants.SINE.get(yaw)

    val yawCos: Int
        get() = Constants.COSINE.get(yaw)
}
