package utils

interface ChunkWriteListener {
    fun onStartWriting(totalSize: Long)
    fun onChunkWritten(written: Long)
    fun onFinishWriting()
}
