package cache.utils

import java.nio.FloatBuffer

fun FloatBuffer.put(x: Float, y: Float, z: Float, w: Float): FloatBuffer =
    put(x).put(y).put(z).put(w)
