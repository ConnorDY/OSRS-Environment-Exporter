package controllers.worldRenderer.helpers

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class GpuFloatBuffer {
    var buffer = allocateDirect(65536)
        private set

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
            newB.put(buffer)
            buffer = newB
        }
    }

    companion object {
        fun allocateDirect(size: Int): FloatBuffer {
            return ByteBuffer.allocateDirect(size * java.lang.Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        }
    }
}
