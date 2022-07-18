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
    private var chunk = ByteArray(INITIAL_CAPACITY)
    private var chunkWrapped = wrapBytes(chunk)

    // Position in the vector we are writing to (e.g. 0th, 1st, or 2nd dimension)
    private var pos = 0

    // Total valid size of this buffer as a whole, in vectors
    var size = 0
        private set

    // Total valid size of the current chunk, in vectors
    private var innerSize = 0

    private fun wrapBytes(chunk: ByteArray): FloatBuffer =
        ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()

    private fun refreshBuffer() {
        val innerBytes = innerSize * dims * BYTES_IN_A_FLOAT
        val chunkToAdd =
            if (innerBytes == chunk.size) chunk
            else chunk.copyOf(innerBytes)
        buffer.addBytes(chunkToAdd)
        innerSize = 0
        chunk = ByteArray(INITIAL_CAPACITY + size * dims * BYTES_IN_A_FLOAT)
        chunkWrapped = wrapBytes(chunk)
    }

    fun add(value: Float) {
        chunkWrapped.put(value)
        min[pos] = min(min[pos], value)
        max[pos] = max(max[pos], value)
        pos++
        if (pos == dims) {
            pos = 0
            size++
            innerSize++

            if ((innerSize + 1) * dims * BYTES_IN_A_FLOAT > chunk.size) {
                refreshBuffer()
            }
        }
    }

    /** Retrieve the raw bytes from this buffer.
     *  Note that this buffer cannot be added to after this operation has taken place.
     */
    fun getBytes(): ByteArray {
        if (innerSize != 0) {
            buffer.addBytes(chunk.copyOf(innerSize * dims * BYTES_IN_A_FLOAT))
            // Ensure no further writes succeed
            chunk = USELESS_ARRAY
            chunkWrapped = wrapBytes(chunk)
            innerSize = 0
        }
        return buffer.getBytes()
    }

    companion object {
        const val INITIAL_CAPACITY = 3 * 512 // Kind of arbitrary
        const val BYTES_IN_A_FLOAT = 4
        val USELESS_ARRAY = ByteArray(0)
    }
}
