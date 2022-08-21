package utils

import java.nio.ByteBuffer

class ByteChunkBuffer {
    private val byteChunks = ArrayList<ByteBuffer>()

    val byteLength get() = byteChunks.sumOf { it.limit().toLong() }

    fun getBuffers(): List<ByteBuffer> = byteChunks.map { it.duplicate() }

    fun addBytes(bytesToAdd: ByteBuffer) {
        byteChunks.add(bytesToAdd)
    }

    fun addBytes(byteChunks: ByteChunkBuffer) {
        this.byteChunks.addAll(byteChunks.byteChunks)
    }
}
