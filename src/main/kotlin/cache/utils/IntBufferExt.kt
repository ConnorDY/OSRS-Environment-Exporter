package cache.utils

import java.nio.IntBuffer

fun IntBuffer.put(x: Int, y: Int, z: Int, w: Int): IntBuffer =
    put(x).put(y).put(z).put(w)
