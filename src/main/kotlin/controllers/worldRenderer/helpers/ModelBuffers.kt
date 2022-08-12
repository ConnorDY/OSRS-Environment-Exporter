package controllers.worldRenderer.helpers

class ModelBuffers {
    fun clearVertUv() {
        vertexBuffer.clear()
        uvBuffer.clear()
    }

    fun flipVertUv() {
        vertexBuffer.flip()
        uvBuffer.flip()
    }

    fun clear() {
        modelBuffer.clear()
        modelBufferSmall.clear()
        modelBufferUnordered.clear()
        unorderedModelsCount = 0
        largeModelsCount = 0
        smallModelsCount = 0
        tempOffset = 0
        tempUvOffset = 0
    }

    fun flip() {
        modelBuffer.flip()
        modelBufferSmall.flip()
        modelBufferUnordered.flip()
    }

    fun bufferForTriangles(triangles: Int): GpuIntBuffer {
        return if (triangles <= SMALL_TRIANGLE_COUNT) {
            ++smallModelsCount
            modelBufferSmall
        } else {
            ++largeModelsCount
            modelBuffer
        }
    }

    val vertexBuffer: GpuIntBuffer = GpuIntBuffer()
    val uvBuffer: GpuFloatBuffer = GpuFloatBuffer()
    val modelBufferUnordered: GpuIntBuffer = GpuIntBuffer()
    val modelBufferSmall: GpuIntBuffer = GpuIntBuffer()
    val modelBuffer: GpuIntBuffer = GpuIntBuffer()

    var unorderedModelsCount = 0
    fun incUnorderedModels() {
        unorderedModelsCount++
    }

    /**
     * number of models in small buffer
     */
    var smallModelsCount = 0

    /**
     * number of models in large buffer
     */
    var largeModelsCount = 0

    /**
     * offset in the target buffer for model
     */
    var targetBufferOffset = 0
    fun addTargetBufferOffset(n: Int) {
        targetBufferOffset += n
    }

    /**
     * offset into the temporary scene vertex buffer
     */
    var tempOffset = 0

    /**
     * offset into the temporary scene uv buffer
     */
    var tempUvOffset = 0

    fun calcPickerId(x: Int, y: Int, objType: Int): Int {
        // pack x tile in top 13 bits, y in next 13, objectId in bottom 5
        // NOTE: signed int so x can really only use 13 bits!!
        return x and 0x1FFF shl 18 or (y and 0x1FFF shl 5) or (objType and 0x1F)
    }

    companion object {
        const val FLAG_SCENE_BUFFER = Int.MIN_VALUE
        const val MAX_TRIANGLE = 4096
        const val SMALL_TRIANGLE_COUNT = 512
    }
}
