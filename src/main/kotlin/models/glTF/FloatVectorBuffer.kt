package models.glTF

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

class FloatVectorBuffer(val dims: Int) {
    val min = FloatArray(dims) { Float.POSITIVE_INFINITY }
    val max = FloatArray(dims) { Float.NEGATIVE_INFINITY }

    private val buffer = Buffer("") // Steal our efficient byte-concat code
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
        chunk = newBuffer(INITIAL_CAPACITY + bufferedSize * BYTES_IN_A_FLOAT)
        chunkWrapped = chunk.asFloatBuffer()
    }

    fun add(value: Float) {
        chunkWrapped.put(value)
        min[pos] = min(min[pos], value)
        max[pos] = max(max[pos], value)
        pos++
        if (pos == dims) {
            pos = 0

            if ((chunkWrapped.position() + dims) * BYTES_IN_A_FLOAT > chunk.limit()) {
                refreshBuffer()
            }
        }
    }

    /** Retrieve the raw bytes from this buffer.
     *  Note that this buffer cannot be added to after this operation has taken place.
     */
    fun getBytes(): ByteBuffer {
        val unflushedFloats = chunkWrapped.position()
        if (unflushedFloats != 0) {
            buffer.addBytes(chunk.limit(unflushedFloats * BYTES_IN_A_FLOAT))
            bufferedSize += unflushedFloats
            // Ensure no further writes succeed
            chunk = USELESS_BUFFER
            chunkWrapped = chunk.asFloatBuffer()
        }
        return buffer.getByteBuffer()
    }

    companion object {
        const val INITIAL_CAPACITY = 3 * 512 // Kind of arbitrary
        const val BYTES_IN_A_FLOAT = Float.SIZE_BYTES
        val USELESS_BUFFER = ByteBuffer.wrap(ByteArray(0))
    }
}
