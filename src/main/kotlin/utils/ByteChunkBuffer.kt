package utils

import java.nio.ByteBuffer

class ByteChunkBuffer {
    private val byteChunks = ArrayList<ByteBuffer>()

    val byteLength get() = byteChunks.sumOf { it.limit() }

    fun getByteBuffer(): ByteBuffer {
        val finalBytes = ByteBuffer.allocateDirect(byteLength)
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
