package controllers.worldRenderer.helpers

import org.lwjgl.BufferUtils
import org.slf4j.LoggerFactory
import java.nio.BufferOverflowException
import java.nio.IntBuffer

class GpuIntBuffer {
    private val logger = LoggerFactory.getLogger(GpuIntBuffer::class.java)

    var buffer = allocateDirect(65536)
        private set

    fun flip() {
        if (buffer.position() == 0) {
            buffer.limit(0)
            return
        }  // TODO: this is only here to make the debugger happy
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
            } catch (e: BufferOverflowException) {
                logger.error("Could not append to GPU buffer", e)
            }

            buffer = newB
        }
    }

    companion object {
        fun allocateDirect(size: Int): IntBuffer =
            BufferUtils.createIntBuffer(size)
    }
}
