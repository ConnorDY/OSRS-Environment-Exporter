package utils

import java.nio.ByteBuffer
import java.util.function.IntFunction

class ByteChunkBuffer(private val factory: IntFunction<ByteBuffer>) {
    private val byteChunks = ArrayList<ByteBuffer>()

    val byteLength get() = byteChunks.sumOf { it.limit() }

    fun getByteBuffer(): ByteBuffer {
        if (byteChunks.size == 1) return byteChunks[0].duplicate()

        val finalBytes = factory.apply(byteLength)
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
