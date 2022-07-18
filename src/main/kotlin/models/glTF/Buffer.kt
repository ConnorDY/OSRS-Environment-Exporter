package models.glTF

import com.fasterxml.jackson.annotation.JsonIgnore

class Buffer(filename: String) {
    val uri = "$filename.bin"

    fun getByteLength() = byteChunks.sumOf { it.size }

    private val byteChunks = ArrayList<ByteArray>()

    @JsonIgnore
    fun getBytes(): ByteArray {
        val finalBytes = ByteArray(getByteLength())
        byteChunks.fold(0) { pos, chunk ->
            System.arraycopy(chunk, 0, finalBytes, pos, chunk.size)
            pos + chunk.size
        }

        // Might as well cache the fruit of our efforts
        byteChunks.clear()
        byteChunks.add(finalBytes)

        return finalBytes
    }

    fun addBytes(bytesToAdd: ByteArray) {
        byteChunks.add(bytesToAdd)
    }
}
