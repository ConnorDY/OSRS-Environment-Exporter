package controllers.worldRenderer.helpers

import com.jogamp.opengl.util.GLBuffers
import org.slf4j.LoggerFactory
import java.nio.BufferOverflowException
import java.nio.IntBuffer

class GpuIntBuffer {
    private val logger = LoggerFactory.getLogger(GpuIntBuffer::class.java)

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
        var capacity = buffer.capacity()
        val position = buffer.position()
        if (capacity - position < size) {
            do {
                capacity *= 2
            } while (capacity - position < size)
            val newB = allocateDirect(capacity)
            buffer.flip()
            try {
                newB.put(buffer)
            } catch(e: BufferOverflowException) {
                logger.error("Could not append to GPU buffer", e)
            }

            buffer = newB
        }
    }

    companion object {
        fun allocateDirect(size: Int): IntBuffer {
            return GLBuffers.newDirectIntBuffer(size * GLBuffers.SIZEOF_INT)
        }
    }
}