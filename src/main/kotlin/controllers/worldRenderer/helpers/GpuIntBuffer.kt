package controllers.worldRenderer.helpers

import com.jogamp.opengl.util.GLBuffers
import java.nio.IntBuffer

class GpuIntBuffer {
    var buffer = allocateDirect(65536)
        private set

    fun put(x: Int, y: Int, z: Int) {
        buffer.put(x).put(y).put(z)
    }

    fun put(x: Int, y: Int, z: Int, c: Int) {
        buffer.put(x).put(y).put(z).put(c)
    }

    fun flip() {
        buffer.flip()
    }

    fun clear() {
        buffer.clear()
    }

    fun ensureCapacity(size: Int) {
        while (buffer.remaining() < size) {
            val newB = allocateDirect(buffer.capacity() * 2)
            buffer.flip()
            newB.put(buffer)
            buffer = newB
        }
    }

    companion object {
        fun allocateDirect(size: Int): IntBuffer {
            return GLBuffers.newDirectIntBuffer(size * GLBuffers.SIZEOF_INT)
        }
    }
}