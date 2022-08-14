package controllers.worldRenderer.helpers

import org.lwjgl.BufferUtils
import java.nio.FloatBuffer

class GpuFloatBuffer {
    var buffer = allocateDirect(65536)
        private set

    fun flip() {
        if (buffer.position() == 0) {
            buffer.limit(0)
            return
        } // TODO: this is only here to make the debugger happy
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
            newB.put(buffer)
            buffer = newB
        }
    }

    companion object {
        fun allocateDirect(size: Int): FloatBuffer =
            BufferUtils.createFloatBuffer(size)
    }
}
