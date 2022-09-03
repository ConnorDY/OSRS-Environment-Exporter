package utils

interface ChunkWriteListener {
    fun onStartRegion(regionNum: Int)
    fun onEndRegion()
    fun onFinishWriting()
}
