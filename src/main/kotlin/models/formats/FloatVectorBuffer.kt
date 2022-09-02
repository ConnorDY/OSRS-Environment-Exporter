package models.formats

import utils.ByteChunkBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

class FloatVectorBuffer(val dims: Int) {
    val min = FloatArray(dims) { Float.POSITIVE_INFINITY }
    val max = FloatArray(dims) { Float.NEGATIVE_INFINITY }

    private val buffer = ByteChunkBuffer() // Steal our efficient byte-concat code
    private var chunk = newBuffer(INITIAL_CAPACITY)
    private var chunkWrapped = chunk.asFloatBuffer()

    // Position in the vector we are writing to (e.g. 0th, 1st, or 2nd dimension)
    private var pos = 0

    // Total valid size of this buffer as a whole, in vectors
    val size get() = (bufferedSize + chunkWrapped.position()) / dims

    private var bufferedSize = 0

    private fun newBuffer(capacity: Int): ByteBuffer =
        ByteBuffer.allocateDirect(capacity).order(ByteOrder.LITTLE_ENDIAN)

    private fun refreshBuffer() {
        val unflushedFloats = chunkWrapped.position()
        val unflushedBytes = unflushedFloats * BYTES_IN_A_FLOAT
        buffer.addBytes(chunk.limit(unflushedBytes))
        bufferedSize += unflushedFloats
        val capacity = INITIAL_CAPACITY + bufferedSize * BYTES_IN_A_FLOAT
        chunk = newBuffer(if (capacity < INITIAL_CAPACITY) 1024 * 1024 * 1024 else capacity)
        chunkWrapped = chunk.asFloatBuffer()
    }

    fun add(value: Float) {
        chunkWrapped.put(value)
        min[pos] = min(min[pos], value)
        max[pos] = max(max[pos], value)

        pos++
        if (pos == dims) {
            pos = 0
            checkBufferCapacity()
        }
    }

    fun add(x: Float, y: Float, z: Float) {
        assert(pos == 0 && dims == 3)
        chunkWrapped.put(x).put(y).put(z)

        min[0] = min(min[0], x)
        max[0] = max(max[0], x)
        min[1] = min(min[1], y)
        max[1] = max(max[1], y)
        min[2] = min(min[2], z)
        max[2] = max(max[2], z)

        checkBufferCapacity()
    }

    fun add(x: Float, y: Float, z: Float, w: Float) {
        assert(pos == 0 && dims == 4)
        chunkWrapped.put(x).put(y).put(z).put(w)

        min[0] = min(min[0], x)
        max[0] = max(max[0], x)
        min[1] = min(min[1], y)
        max[1] = max(max[1], y)
        min[2] = min(min[2], z)
        max[2] = max(max[2], z)
        min[3] = min(min[3], w)
        max[3] = max(max[3], w)

        checkBufferCapacity()
    }

    private fun checkBufferCapacity() {
        if ((chunkWrapped.position() + dims) * BYTES_IN_A_FLOAT > chunk.limit()) {
            refreshBuffer()
        }
    }

    /** Retrieve the raw bytes from this buffer.
     *  Note that this buffer cannot be added to after this operation has taken place.
     */
    fun getByteChunks(): ByteChunkBuffer {
        val unflushedFloats = chunkWrapped.position()
        if (unflushedFloats != 0) {
            buffer.addBytes(chunk.limit(unflushedFloats * BYTES_IN_A_FLOAT))
            bufferedSize += unflushedFloats
            // Ensure no further writes succeed
            chunk = USELESS_BUFFER
            chunkWrapped = chunk.asFloatBuffer()
        }
        return buffer
    }

    companion object {
        const val INITIAL_CAPACITY = 3 * 512 // Kind of arbitrary
        const val BYTES_IN_A_FLOAT = Float.SIZE_BYTES
        val USELESS_BUFFER = ByteBuffer.wrap(ByteArray(0))
    }
}
