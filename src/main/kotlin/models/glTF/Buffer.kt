package models.glTF

import com.fasterxml.jackson.annotation.JsonIgnore
import java.nio.ByteBuffer

class Buffer(filename: String) {
    val uri = "$filename.bin"

    fun getByteLength() = byteChunks.sumOf { it.limit() }

    private val byteChunks = ArrayList<ByteBuffer>()

    @JsonIgnore
    fun getBytes(): ByteArray {
        val buf = getByteBuffer()
        val arr = ByteArray(buf.remaining())
        buf.get(arr)
        return arr
    }

    @JsonIgnore
    fun getByteBuffer(): ByteBuffer {
        val finalBytes = ByteBuffer.allocateDirect(getByteLength())
        byteChunks.forEach { chunk ->
            finalBytes.put(chunk)
        }
        finalBytes.flip()

        // Might as well cache the fruit of our efforts
        byteChunks.clear()
        byteChunks.add(finalBytes.duplicate())
        return finalBytes
    }

    fun addBytes(bytesToAdd: ByteBuffer) {
        byteChunks.add(bytesToAdd)
    }
}
