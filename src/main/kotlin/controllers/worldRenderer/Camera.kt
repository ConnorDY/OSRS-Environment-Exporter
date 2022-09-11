package controllers.worldRenderer

class Camera {
    var pitchRads: Double = Math.PI * 0.2
    var yawRads: Double = 0.0
    var scale = 550
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

    fun addYaw(amt: Double) {
        yawRads = (yawRads + amt).mod(Math.PI * 2)
    }

    fun addPitch(amt: Double) {
        val amtClamped = amt.coerceIn(-Math.PI * 0.5, Math.PI * 0.5)

        var newPitch = (pitchRads + amtClamped)
        if (newPitch > Math.PI * 0.5 && newPitch < Math.PI * 1.5) {
            // Disallow upside-down camera; clamp to 90 degrees up or down
            newPitch = if (amt > 0) {
                Math.PI * 0.5
            } else {
                Math.PI * 1.5
            }
        }
        pitchRads = newPitch.mod(Math.PI * 2)
    }
}
